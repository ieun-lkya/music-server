package com.music.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    private Integer code;
    private String msg;
    private T data;

    // 成功时的快捷方法
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "请求成功", data);
    }

    // 失败时的快捷方法
    public static Result error(String msg) {
        return new Result<>(500, msg, null);
    }
}