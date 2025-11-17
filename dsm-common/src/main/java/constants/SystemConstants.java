package constants;

/**
 * 系统通用常量
 */
public class SystemConstants {
    // 数据库相关
    public static final String DB_PREFIX = "music_"; // 表名前缀
    public static final int BATCH_INSERT_SIZE = 100; // 批量插入批次大小

    // 接口性能阈值
    public static final int API_RESPONSE_MAX = 300; // 核心接口响应上限(ms)
    public static final int PAGE_LOAD_MAX = 1500; // 页面加载上限(ms)
    public static final int AUDIO_BUFFER_MAX = 1000; // 音频缓冲上限(ms)

    // 限流配置
    public static final int SEckILL_QPS = 500; // 秒杀接口QPS阈值
    public static final int SECKILL_IP_LIMIT = 1; // 单IP秒杀次数限制

    // 时间相关
    public static final int JWT_EXPIRE_HOURS = 2; // JWT过期时间(小时)
    public static final String REPORT_GENERATE_TIME = "02:00:00"; // 听歌报告生成时间
    public static final int DATA_DELAY_MAX = 30; // 数据延迟上限(分钟)

    // 存储相关
    public static final String UNCHECKED_FILE_PRIVATE = "private"; // 未审核文件权限
    public static final String FILE_STORAGE_PREFIX = "audio/"; // 音频存储路径前缀

    //文件大小相关
    public static final int MAX_PICTURE_SIZE = 1024 * 1024 * 10;
    public static final int MAX_MUSIC_SIZE = 1024 * 1024 * 100;
    public static final int MAX_LYRIC_SIZE = 1024 * 1024;
}