package com.future.music.constants;

public class MusicVoConstants {
    public static final String MUSIC_ID = "id";
    public static final String SONG_NAME = "songName";
    public static final String SINGER = "singer";
    public static final String COVER_URL = "coverUrl";

    public static final String REDIS_ZSET_HOT_MUSIC = "recommend:hot:music"; // 热门歌曲ZSet（score=热门得分）
    public static final long HOT_MUSIC_REFRESH_HOURS = 1;                    // 热门歌曲每1小时刷新一次
}
