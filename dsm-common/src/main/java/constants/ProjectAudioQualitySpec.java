package constants;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 项目专属音质规格（对应：1-标准、2-高清、3-无损、4-空间音频）
 */
public class ProjectAudioQualitySpec {
    // 1-标准音质（128kbps MP3）
    public static final Spec STANDARD = new Spec(1, "标准音质",
            44100, 16, new String[]{"MP3"}, 128, 2);
    // 2-高清音质（320kbps MP3/AAC）
    public static final Spec HIGH_DEF = new Spec(2, "高清音质",
            44100, 16, new String[]{"MP3", "AAC"}, 320, 2);
    // 3-无损音质（Lossless FLAC/WAV）
    public static final Spec LOSSLESS = new Spec(3, "无损音质",
            44100, 16, new String[]{"FLAC", "WAV"}, 700, 2);
    // 4-空间音频（96000Hz 24bit FLAC/WAV）
    public static final Spec SPATIAL_AUDIO = new Spec(4, "空间音频",
            96000, 24, new String[]{"FLAC", "WAV"}, 2000, 2);

    @Data
    @AllArgsConstructor
    public static class Spec {
        private int typeCode;       // 音质类型编码（1-4）
        private String typeName;    // 音质名称
        private int minSampleRate;  // 最小采样率（Hz）
        private int minBitDepth;    // 最小位深（bit）
        private String[] allowFormats; // 允许的编码格式
        private int minBitRate;     // 最小码率（bps）
        private int minChannels;    // 最小声道数（空间音频需要多声道）
    }

    /**
     * 根据类型编码获取对应的规格
     */
    public static Spec getSpecByCode(int typeCode) {
        return switch (typeCode) {
            case 1 -> STANDARD;
            case 2 -> HIGH_DEF;
            case 3 -> LOSSLESS;
            case 4 -> SPATIAL_AUDIO;
            default -> throw new IllegalArgumentException("不支持的音质类型编码：" + typeCode);
        };
    }
}