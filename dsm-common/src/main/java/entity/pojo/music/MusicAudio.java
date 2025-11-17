package entity.pojo.music;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("music_audio_relation")
public class MusicAudio {
    private Long id;
    private Long musicId;           //音乐id
    private Integer qualityType;    //音质类型（1-标准，2-高清，3-无损，4-空间音频）
    private String audioUrl;        //音频URL
    private Long fileSize;          //文件大小（字节）
}
