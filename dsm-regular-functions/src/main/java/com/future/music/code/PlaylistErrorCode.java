package com.future.music.code;

import lombok.Data;
import lombok.Getter;

@Getter
public enum PlaylistErrorCode {

    PLAYLIST_NOT_EXIST("10001", "歌单不存在"),
    PLAYLIST_NOT_AUTHORIZED("10002", "歌单无权限"),
    PLAYLIST_NOT_ALLOW_ADD_SONG("10003", "歌单不允许添加歌曲"),
    PLAYLIST_NOT_ALLOW_DELETE_SONG("10004", "歌单不允许删除歌曲"),
    PLAYLIST_NOT_ALLOW_UPDATE_SONG("10005", "歌单不允许更新歌曲"),
    PLAYLIST_NAME_EXIST("20001", "歌单名称已存在")
    ;
    private String code;
    private String message;

    PlaylistErrorCode(String code, String msg) {
    }
}
