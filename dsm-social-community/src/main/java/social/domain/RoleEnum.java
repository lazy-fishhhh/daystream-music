package social.domain;

import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public enum RoleEnum {
    USER(1,"普通用户"),
    VIP(2,"VIP用户"),
    MUSICIAN(3,"音乐人"),
    ADMIN(4,"管理员");
    private final Integer value;
    private final String doc;
    public Integer getValue() {
        return value;
    }
    public String getDoc() {
        return doc;
    }
    public static RoleEnum valueOf(Integer value) {
        for (RoleEnum roleEnum : RoleEnum.values()) {
            if (roleEnum.getValue().equals(value)) {
                return roleEnum;
            }
        }
        return null;
    }
    public static RoleEnum docOf(String doc) {
        for (RoleEnum roleEnum : RoleEnum.values()) {
            if (roleEnum.getDoc().equals(doc)) {
                return roleEnum;
            }
        }
        return null;
    }
    public static List<String> docs(List<Integer> values)
    {
        List<String> docs = new ArrayList<>();
        for(RoleEnum roleEnum : RoleEnum.values())
        {
           for(Integer value : values)
           {
               if(roleEnum.getValue().equals(value))
               {
                   docs.add(roleEnum.getDoc());
               }
           }
        }
        return docs;
    }
}
