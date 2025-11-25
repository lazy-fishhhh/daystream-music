package entity.pojo.music;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("music_lyric")
public class MusicLyric implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联音乐ID */
    private Long musicId;

    /** 歌词文件地址（MinIO） */
    private String lyricUrl;

    /** 歌词原始内容 */
    private String lyricContent;

    /** 歌词格式：lrc/txt/krc */
    private String lyricType;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}