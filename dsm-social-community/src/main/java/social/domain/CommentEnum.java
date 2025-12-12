package social.domain;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum CommentEnum {
    MUSIC(1,"音乐"),
    PLAYLIST(2,"歌单"),
    ALBUM(3,"专辑"),
    MV(4,"MV"),
    COMMUNITY(5,"社区");
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
    public static CommentEnum getCommentByValue(Integer va)
    {
        for(CommentEnum value:values())
        {
            if(value.getValue().equals(va))
            {
                return value;
            }
        }
        return null;
    }
    public static CommentEnum getCommentByDes(String des)
    {
        for(CommentEnum value:values())
        {
            if(value.getDes().equals(des))
            {
                return value;
            }
        }
        return null;
    }
}
