package constants;

import java.time.format.DateTimeFormatter;

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
    // 周标识格式：yyyy-ww（ww为周数，1-53）
    public static final DateTimeFormatter WEEK_FORMATTER = DateTimeFormatter.ofPattern("yyyy-ww"); // 周标识格式化器



    // 存储相关
    public static final String UNCHECKED_FILE_PRIVATE = "private"; // 未审核文件权限
    public static final String FILE_STORAGE_PREFIX = "audio/"; // 音频存储路径前缀

    //文件大小相关
    public static final int MAX_PICTURE_SIZE = 1024 * 1024 * 10;
    public static final int MAX_MUSIC_SIZE = 1024 * 1024 * 200;
    public static final int MAX_LYRIC_SIZE = 1024 * 1024;

    // 搜索类型
    public static final int MUSIC_SEARCH_TYPE = 1;      // 音乐
    public static final int PLAYLIST_SEARCH_TYPE = 2;   // 歌单
    public static final int ALBUM_SEARCH_TYPE = 3;      // 专辑
    public static final int MUSICIAN_SEARCH_TYPE = 4;   // 音乐人
    public static final int MV_SEARCH_TYPE = 5;         // MV
    public static final int USER_SEARCH_TYPE = 6;       // 用户
    public static final int HOT_SEARCH_LIMIT = 10;      // 热搜榜单限制数量
}