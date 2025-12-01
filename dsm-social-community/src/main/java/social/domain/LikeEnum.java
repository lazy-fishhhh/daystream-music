package social.domain;

import lombok.AllArgsConstructor;
@AllArgsConstructor
public enum LikeEnum {
    MUSIC(1,"音乐"),
    COMMENT(2,"评论"),
    COMMUNITY(3,"动态");
    private final Integer value;
    private final String des;
    public Integer getValue()
    {
        return value;
    }
    public String getDes()
    {
        return des;
    }
    public static LikeEnum getLikeEnumBydes(String des)
    {
        for(LikeEnum value:values())
        {
            if(value.getDes().equals(des))
            {
                return value;
            }
        }
        return null;
    }

    public static LikeEnum getLikeEnumByvalue(Integer va)
    {
        for(LikeEnum value:values())
        {
            if(value.getValue().equals(va))
            {
                return value;
            }
        }
        return null;
    }
}
