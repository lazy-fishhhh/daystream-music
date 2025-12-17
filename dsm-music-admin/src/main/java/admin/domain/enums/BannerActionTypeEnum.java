package admin.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * Banner操作类型枚举
 * 用于定义Banner的交互行为类型
 */
@Getter
public enum BannerActionTypeEnum {
    /**
     * 无操作
     */
    NONE(0, "无操作"),
    /**
     * 跳转到歌单
     */
    TO_PLAYLIST(1, "跳转到歌单"),


    TO_SONG(2, "跳转到歌曲"),


    /**
     * 跳转到活动
     */
    TO_ACTIVITY(3, "跳转到活动"),


    /**
     * 跳转到歌手
     */
    TO_SINGER(6, "跳转到歌手"),
    /**
     * 跳转到专辑
     */
    TO_ALBUM(7, "跳转到专辑"),
    /**
     * 跳转到MV
     */
    TO_MV(8, "跳转到MV");

    /**
     * 枚举值
     */
    @JsonValue
    private final Integer value;

    /**
     * 描述
     */
    private final String description;

    BannerActionTypeEnum(Integer value, String description) {
        this.value = value;
        this.description = description;
    }

    /**
     * 根据枚举值获取枚举
     * @param value 枚举值
     * @return 枚举
     */
    public static BannerActionTypeEnum getByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (BannerActionTypeEnum enumItem : BannerActionTypeEnum.values()) {
            if (enumItem.getValue().equals(value)) {
                return enumItem;
            }
        }
        return null;
    }
}
