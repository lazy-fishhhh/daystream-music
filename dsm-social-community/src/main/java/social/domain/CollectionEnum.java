package social.domain;


import lombok.AllArgsConstructor;
import org.springframework.security.core.parameters.P;

@AllArgsConstructor
public enum CollectionEnum {
    MUSIC(1,"音乐"),
    PLAYLIST(2,"歌单");
    private Integer value;
    private String des;
    public Integer getValue()
    {
        return value;
    }
    public String getDes()
    {
        return  des;
    }
    public static CollectionEnum getCollectByValue(Integer va)
    {
        for(CollectionEnum value:values())
        {
            if(value.getValue()==va)
            {
                return value;
            }
        }
        return null;
    }
    public static CollectionEnum getCollectByDes(String des)
    {
        for(CollectionEnum value:values())
        {
            if(value.des==des)
            {
                return value;
            }
        }
        return null;
    }

}
