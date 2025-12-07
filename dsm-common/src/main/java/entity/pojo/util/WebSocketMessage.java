package entity.pojo.util;

import lombok.Data;

// 用于封装消息数据
@Data

public class WebSocketMessage {

    // 是否是系统消息（比如系统通知、公告）
    private boolean system;
    // 消息内容（可以是文本、对象等）
    private Object message;
    // 发送者名称（非系统消息时，是用户昵称/ID）
    private Long fromUser;
    private Long toUser;


}