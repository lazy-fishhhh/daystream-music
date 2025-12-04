package admin.domain;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum MusicianAdminEnum {
    PENDING(0,"待审核"),
    APPROVED(1,"审核通过"),
    REJECTED(2,"审核拒绝");
    private final Integer code;
    private final String desc;
    public Integer getCode() {
        return code;
    }
    public String getDesc() {
        return desc;
    }
    public static String getDescByCode(Integer code) {
        for (MusicianAdminEnum value : MusicianAdminEnum.values()) {
            if (value.code.equals(code)) {
                return value.desc;
            }
        }
        return null;
    }
    public static Integer getCodeByDesc(String desc) {
        for (MusicianAdminEnum value : MusicianAdminEnum.values()) {
            if (value.desc.equals(desc)) {
                return value.code;
            }
        }
        return null;
    }
}
