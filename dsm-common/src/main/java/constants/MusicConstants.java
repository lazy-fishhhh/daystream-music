package constants;

/**
 * 音乐、音质、格式相关常量
 */
public class MusicConstants {
    // 音质类型
    public static final String AUDIO_QUALITY_STANDARD = "STANDARD"; // 标准音质
    public static final String AUDIO_QUALITY_HD = "HD"; // 高清音质
    public static final String AUDIO_QUALITY_LOSSLESS = "LOSSLESS"; // 无损音质
    public static final String AUDIO_QUALITY_SPATIAL = "SPATIAL"; // 空间音频
    public static final String AUDIO_QUALITY_MASTER = "MASTER"; // 母带音质

    // 音频格式
    public static final String AUDIO_FORMAT_MP3 = "MP3";
    public static final String AUDIO_FORMAT_WAV = "WAV";
    public static final String AUDIO_FORMAT_FLAC = "FLAC";

    // 播放模式
    public static final String PLAY_MODE_ORDER = "ORDER"; // 顺序播放
    public static final String PLAY_MODE_SINGLE = "SINGLE"; // 单曲循环
    public static final String PLAY_MODE_RANDOM = "RANDOM"; // 随机播放

    // 筛选维度
    public static final String FILTER_DIMENSION_GENRE = "GENRE"; // 流派
    public static final String FILTER_DIMENSION_MOOD = "MOOD"; // 心情
    public static final String FILTER_DIMENSION_BPM = "BPM"; // 节奏
    public static final String FILTER_DIMENSION_INSTRUMENT = "INSTRUMENT"; // 乐器
}