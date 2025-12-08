package entity.pojo.util;

import com.alibaba.fastjson2.JSON;

public class WebSocketMessageUtils {

    public static String getMessage(boolean isSystemMessage, Long fromUser, Object message) {
        WebSocketMessage result = new WebSocketMessage();
        result.setSystem(isSystemMessage);
        result.setMessage(message);
        if (fromUser != null) {
            result.setFromUser(fromUser);
        }
        return JSON.toJSONString(result);
    }
}