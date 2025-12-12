package social.domain;


import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum CollectionEnum {
    PLAYLIST(1,"歌单"),
    AlBUM(2,"专辑"),
    MV(3,"MV");
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
