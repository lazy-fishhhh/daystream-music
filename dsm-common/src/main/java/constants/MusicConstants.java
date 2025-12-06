package constants;

import java.util.regex.Pattern;

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

    //音乐统计时长阈值（秒）
    public static final int MIN_PLAY_SECONDS = 3;
    // 播放达标率（播放时长占总时长的比例）
    public static final double MIN_PLAY_RATIO = 0.3;

    // 缓存 Key 前缀 + 过期时间（1小时，可根据业务调整）
    public static final String CACHE_KEY_MUSIC_DETAIL = "music:detail:";
    public static final long CACHE_TTL_SECONDS = 3600;

    /**
     * 匹配连续的空白字符（包括空格、制表符、换页符等）
     */
    public static final Pattern LINE_BLANK_PATTERN = Pattern.compile("[ \\t\\f\\u00A0]+");
    public static final Pattern CRLF_PATTERN = Pattern.compile("\\r\\n");
    public static final Pattern CONTROL_CHAR_PATTERN = Pattern.compile("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]");
    public static final Pattern LINE_EDGE_SPACE_PATTERN = Pattern.compile("(?m)^\\s+|\\s+$");
}