package com.typenglish.common;

import lombok.Data;

/**
 * 统一响应格式
 */
@Data
public class Result<T> {

    /** 状态码: 200=成功, 其他=错误 */
    private int code;

    /** 提示信息 */
    private String msg;

    /** 响应数据 */
    private T data;

    private Result(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    // ---- 成功 ----

    public static <T> Result<T> ok() {
        return new Result<>(200, "ok", null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(200, "ok", data);
    }

    public static <T> Result<T> ok(String msg, T data) {
        return new Result<>(200, msg, data);
    }

    // ---- 失败 ----

    public static <T> Result<T> fail(String msg) {
        return new Result<>(500, msg, null);
    }

    public static <T> Result<T> fail(int code, String msg) {
        return new Result<>(code, msg, null);
    }

    // ---- 常用 ----

    public static <T> Result<T> badRequest(String msg) {
        return new Result<>(400, msg, null);
    }

    public static <T> Result<T> unauthorized(String msg) {
        return new Result<>(401, msg, null);
    }

    public static <T> Result<T> notFound(String msg) {
        return new Result<>(404, msg, null);
    }
}
