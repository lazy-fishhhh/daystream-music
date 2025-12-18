package util;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * 音频时长工具类，支持解析主流音频格式（MP3、FLAC、WAV、AAC等）的时长
 */
public class AudioDurationUtil {


    /**
     * 解析MultipartFile类型的音频文件时长，返回整数秒数
     * @param multipartFile 音频文件（MP3/FLAC等）
     * @return 时长（秒）：解析成功返回正整数，失败返回-1，文件为空返回-2
     */
    public static int getAudioDuration(MultipartFile multipartFile) {
        // 空文件直接返回-1
        if (multipartFile.isEmpty() || multipartFile.getSize() == 0) {
            return -1;
        }

        File tempFile = null;
        try {
            // 创建临时文件，保留原文件后缀（提升解析兼容性）
            String originalFilename = multipartFile.getOriginalFilename();
            String suffix = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".tmp";
            tempFile = File.createTempFile("audio_temp", suffix);

            // MultipartFile 写入临时文件（使用transferTo更高效）
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(multipartFile.getBytes());
            }

            // 读取音频文件信息
            AudioFile audioFile = AudioFileIO.read(tempFile);
            AudioHeader audioHeader = audioFile.getAudioHeader();

            // 获取时长（秒），转为int类型
            int trackLength = audioHeader.getTrackLength();
            return trackLength > 0 ? trackLength : -1;

        } catch (CannotReadException e) {
            System.err.println("不支持的音频格式：" + e.getMessage());
            return -1;
        } catch (InvalidAudioFrameException e) {
            System.err.println("音频文件帧结构异常：" + e.getMessage());
            return -1;
        } catch (ReadOnlyFileException e) {
            System.err.println("文件为只读状态：" + e.getMessage());
            return -1;
        } catch (IOException e) {
            System.err.println("文件IO异常：" + e.getMessage());
            return -1;
        } catch (Exception e) {
            System.err.println("解析音频时长失败：" + e.getMessage());
            return -1;
        } finally {
            // 删除临时文件（确保资源释放）
            if (tempFile != null && tempFile.exists()) {
                // 加个判断，防止删除失败时的日志刷屏
                if (!tempFile.delete()) {
                    System.err.println("临时文件删除失败：" + tempFile.getAbsolutePath());
                }
            }
        }
    }


    /**
     * 将秒数转换为 "mm:ss" 格式（如 61秒 → 01:01）
     * @param totalSeconds 总秒数（≥0，-1表示解析失败）
     * @return 格式化字符串，失败返回 "00:00"
     */
    public static String formatSecondsToMS(long totalSeconds) {
        if (totalSeconds < 0) {
            return "00:00";
        }

        // 计算分钟数（总秒数 ÷ 60）
        long minutes = totalSeconds / 60;
        // 计算剩余秒数（总秒数 % 60）
        long seconds = totalSeconds % 60;

        // 补前导零，确保分钟和秒均为两位数
        return String.format("%02d:%02d", minutes, seconds);
    }


}