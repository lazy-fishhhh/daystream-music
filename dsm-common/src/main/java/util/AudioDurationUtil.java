package util;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * 音频时长工具类，支持解析主流音频格式（MP3、FLAC、WAV、AAC等）的时长
 */
public class AudioDurationUtil {

//    private static final Logger log = LoggerFactory.getLogger(AudioDurationUtil.class);

    /**
     * 解析音频文件时长（单位：秒）
     * @param audioFile 上传的音频文件（MultipartFile）
     * @return 时长（秒），解析失败返回 -1
     */
    public static long getAudioDuration(MultipartFile audioFile) {
        // 校验文件是否为空
        if (audioFile.isEmpty()) {
            return -1;
        }

        FFmpegFrameGrabber grabber = null;
        try (InputStream inputStream = audioFile.getInputStream()) {
            // 初始化FFmpeg解析器
            grabber = new FFmpegFrameGrabber(inputStream);
            grabber.start(); // 启动解析

            // 获取时长（FFmpeg返回微秒，转换为秒）
            long durationMicros = grabber.getLengthInTime();
            if (durationMicros < 0) {
                return -1;
            }

            return durationMicros / 1000000; // 1秒 = 1,000,000微秒

        } catch (FFmpegFrameGrabber.Exception e) {
//            log.error("FFmpeg解析音频时长失败（格式不支持或文件损坏）", e);
        } catch (Exception e) {
//            log.error("解析音频时长时发生未知错误", e);
        } finally {
            // 确保资源关闭
            if (grabber != null) {
                try {
                    grabber.stop();
                } catch (FFmpegFrameGrabber.Exception e) {
//                    log.error("关闭FFmpeg解析器失败", e);
                }
            }
        }

        return -1;
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