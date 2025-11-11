package entity.dto;


import entity.pojo.music.Music;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MusicDTO {
    private String musicName;        // 歌曲名
    private Long albumId;            // 所属专辑ID
    private Long musicianId;         // 音乐人ID
    private Integer bpm;             // 节奏（BPM）
    private String licenseType;      // 版权类型
    private Integer isVip;           // 是否VIP专属（0-否，1-是）
    private BigDecimal price;        // 单价（音浪）

    public Music toEntity(){
        Music music = new Music();
        music.setMusicName(musicName);
        music.setAlbumId(albumId);
        music.setMusicianId(musicianId);
        music.setBpm(bpm);
        music.setLicenseType(licenseType);
        music.setIsVip(isVip);
        music.setPrice(price);
        return music;
    }

    /**
     * 校验DTO所有属性是否合理
     * @return true=校验通过，false=校验失败
     */
    public boolean validate() {
        // 1. 歌曲名：非空且长度1-100
        if (!StringUtils.hasText(musicName) || musicName.length() > 100) {
            return false;
        }
        // 2. 专辑ID：非空且为正数
        if (albumId == null || albumId <= 0) {
            return false;
        }
        // 3. 音乐人ID：非空且为正数
        if (musicianId == null || musicianId <= 0) {
            return false;
        }
        // 4. BPM：非空且范围60-200
        if (bpm == null || bpm < 60 || bpm > 200) {
            return false;
        }
        // 5. 版权类型：非空且只能是「原创」「翻唱」「授权」
        if (!StringUtils.hasText(licenseType)
                || !("原创".equals(licenseType) || "翻唱".equals(licenseType) || "授权".equals(licenseType))) {
            return false;
        }
        // 6. 是否VIP：非空且只能是0或1
        if (isVip == null || (isVip != 0 && isVip != 1)) {
            return false;
        }
        // 7. 单价：非空、非负数且最多2位小数
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0 || price.scale() > 2) {
            return false;
        }
        // 所有校验通过
        return true;
    }
}
