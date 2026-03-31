package entity.dto;


import entity.pojo.music.Music;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MusicDTO {
    private String musicName;        // 歌曲名
    private Long albumId;            // 所属专辑ID
    private Integer bpm;             // 节奏（BPM）
    private String licenseType;      // 版权类型
    private Integer isVip;           // 是否VIP专属（0-否，1-是）
    private BigDecimal price;        // 单价（音浪）
    private List<Long> tags;         // 标签

    /**
     * 将MusicDTO转化为Music
     * @return Music对象
     */
    public Music toEntity(){
        Music music = new Music();
        music.setMusicName(musicName);
        music.setAlbumId(albumId);
        music.setBpm(bpm);
        music.setLicenseType(licenseType);
        music.setIsVip(isVip);
        music.setPrice(price);
        return music;
    }

    /**
     * 上传歌曲时校验DTO所有属性是否合理
     * @return true=校验通过，false=校验失败
     */
    public boolean saveValidate() {
        // 1. 歌曲名：非空且长度1-100
        if (!StringUtils.hasText(musicName) || musicName.length() > 100) {
            return false;
        }
        // 2. 专辑ID：非空且为正数
        if (albumId == null || albumId <= 0) {
            return false;
        }
        // 4. BPM：非空且范围60-200
        if (bpm == null || bpm < 60 || bpm > 200) {
            return false;
        }
        // 5. 版权类型：非空且只能是CC-BY(署名即可免费使用（商用也可）),
        // CC-BY-NC(署名 + 非商用（不能用于盈利场景）),
        // CC-BY-SA(（修改后作品需用同一协议）),
        // CC-BY-ND(署名 + 禁止演绎（不能修改原作品）)
        if (!StringUtils.hasText(licenseType)
                || !("CC-BY".equals(licenseType) || "CC-BY-NC".equals(licenseType) || "CC-BY-SA".equals(licenseType) || "CC-BY-ND".equals(licenseType))) {
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
        // 8. 标签列表校验：非空、元素非空且为正数
        if (tags == null || tags.isEmpty()) {
            return false; // 标签列表不能为空
        }
        for (Long tagId : tags) {
            if (tagId == null || tagId <= 0) {
                return false; // 标签ID不能为null或小于等于0
            }
        }
        // 所有校验通过
        return true;
    }

    /**
     * 更新歌曲时校验DTO所有属性是否合理
     * @return true=校验通过，false=校验失败
     */
    public boolean updateValidate() {
        // 1. 歌曲名：长度1-100
        if (StringUtils.hasText(musicName) && musicName.length() > 100) {
            return false;
        }
        // 2. 专辑ID：为正数
        if (albumId != null && albumId <= 0) {
            return false;
        }
        // 4. BPM：范围60-200
        if (bpm != null && (bpm < 60 || bpm > 200)) {
            return false;
        }
        // 5. 版权类型：只能是CC-BY(署名即可免费使用（商用也可）),CC-BY-NC(署名 + 非商用（不能用于盈利场景）),CC-BY-SA(（修改后作品需用同一协议）),CC-BY-ND(署名 + 禁止演绎（不能修改原作品）)
        if (StringUtils.hasText(licenseType)
                && !("CC-BY".equals(licenseType) || "CC-BY-NC".equals(licenseType) || "CC-BY-SA".equals(licenseType) || "CC-BY-ND".equals(licenseType))) {
            return false;
        }
        // 6. 是否VIP：只能是0或1
        if (isVip != null && (isVip != 0 && isVip != 1)) {
            return false;
        }
        // 7. 单价：非负数且最多2位小数
        if (price != null && (price.compareTo(BigDecimal.ZERO) < 0 || price.scale() > 2)) {
            return false;
        }
        // 8. 标签列表校验：元素为正数
        if (tags != null) {
            if (tags.isEmpty()) {
                return false; // 标签列表不能为空
            }
            for (Long tagId : tags) {
                if (tagId == null || tagId <= 0) {
                    return false; // 标签ID不能为null或小于等于0
                }
            }
        }

        // 所有校验通过
        return true;
    }

}
