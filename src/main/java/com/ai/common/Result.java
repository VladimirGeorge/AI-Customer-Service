package com.ai.common;

import lombok.Data;

import java.io.Serializable;

@Data
public class Result<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer code;
    private String message;
    private T data;

    // 私有构造方法
    private Result() {}

    private Result(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // 成功响应（无数据）
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功");
    }

    // 成功响应（带数据）
    //public static <T> Result<T> success(T data) {
        //return new Result<>(200, "操作成功", data);
    //}

    // 成功响应（自定义消息）
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }

    // 错误响应（自定义状态码和消息）
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message);
    }

    // 错误响应（默认500错误）
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message);
    }

    // 错误响应（枚举类型错误）
    public static <T> Result<T> error(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage());
    }

    // 错误响应（枚举类型错误 + 自定义消息）
    public static <T> Result<T> error(ErrorCode errorCode, String message) {
        return new Result<>(errorCode.getCode(), message);
    }

    // 链式设置数据
    public Result<T> data(T data) {
        this.data = data;
        return this;
    }

    // 链式设置消息
    public Result<T> message(String message) {
        this.message = message;
        return this;
    }

    // 链式设置状态码
    public Result<T> code(Integer code) {
        this.code = code;
        return this;
    }

    // 判断结果是否成功
    public boolean isSuccess() {
        return this.code != null && this.code == 200;
    }

    // 错误码枚举
    public enum ErrorCode {
        SUCCESS(200, "成功"),
        FAILURE(500, "系统错误"),
        PARAM_ERROR(400, "参数错误"),
        UNAUTHORIZED(401, "未授权"),
        FORBIDDEN(403, "禁止访问"),
        NOT_FOUND(404, "资源不存在"),
        TIMEOUT(504, "请求超时");

        private final Integer code;
        private final String message;

        ErrorCode(Integer code, String message) {
            this.code = code;
            this.message = message;
        }

        public Integer getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }
    // 静态工厂方法
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("成功");
        result.setData(data);
        return result;
    }
}