package com.example.music.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * Result 统一返回结果类
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class Result {

    private Boolean success;
    private String errorMsg;
    private Object data;
    private Long total;
    private Integer errCode;

    public static Result ok(){
        return new Result(true, null, null, null,null);
    }
    public static Result ok(Object data){
        return new Result(true, null, data, null, null);
    }
    public static Result ok(List<?> data, Long total){
        return new Result(true, null, data, total, null);
    }
    public static Result fail(String errorMsg){
        return new Result(false, errorMsg, null, null, null);
    }

    public static Result fail(Integer errCode){
        return new Result(false, null, null, null, errCode);
    }
}
