package util;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * 音频时长工具类，支持解析主流音频格式（MP3、FLAC、WAV、AAC等）的时长
 */
public class AudioDurationUtil {


    /**
     * 解析音频文件时长（单位：秒）
     * @param audioFile 上传的音频文件（MultipartFile）
     * @return 时长（秒），解析失败返回 -1
     */
    public static int getAudioDuration(MultipartFile audioFile) {
        // 1. 校验文件
        if (audioFile.isEmpty()) {
            return -1;
        }

        File tempFile = null;
        try {
            // 1. 生成临时文件时指定后缀（让Jaudiotagger更易识别格式）
            String originalFilename = audioFile.getOriginalFilename();
            String suffix = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf(".")) : ".tmp";
            tempFile = File.createTempFile("audio_" + UUID.randomUUID(), suffix);
            // 2. 强制刷新写入，确保文件完全落地
            audioFile.transferTo(tempFile);
            tempFile.setReadable(true); // 确保读权限

            // 3. 验证文件大小（避免空文件/截断文件）
            if (tempFile.length() < 1024) { // 小于1KB的音频文件大概率是损坏的
                return -1;
            }

            // 4. 解析文件
            AudioFile audioFileObj = AudioFileIO.read(tempFile);
            return audioFileObj.getAudioHeader().getTrackLength();

        } catch (CannotReadException e) {
            // 不支持的格式/文件损坏
            return -1;
        } catch (IOException | TagException | ReadOnlyFileException | InvalidAudioFrameException e) {
            // 其他解析异常
            return -1;
        } finally {
            // 5. 删除临时文件，释放磁盘空间
            if (tempFile != null && tempFile.exists()) {
                boolean deleted = tempFile.delete();
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