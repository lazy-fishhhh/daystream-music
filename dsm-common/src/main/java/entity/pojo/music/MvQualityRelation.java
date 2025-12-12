package entity.pojo.music;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MvQualityRelation {
    private Long id;                 // 画质关联ID
    private Long mvId;               // 关联MV ID
    private Integer qualityType;     // 画质类型（1-标清480P，2-高清720P，3-超清1080P，4-4K）
    private String mvUrl;            // 对应画质的MV播放地址
    private Long fileSize;           // 文件大小（字节）
    private Integer isVipLimit;      // 是否仅VIP可播放（0-否，1-是）
}