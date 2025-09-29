package com.sg.nusiss.gamevaultbackend.common;

import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName BaseResponse
 * @Author HUANG ZHENJIA
 * @Date 2025/9/29
 * @Description
 */
@Data
public class BaseResponse<T> implements Serializable {

    private int code;

    private T data;

    private String message;

    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public BaseResponse(int code, T data) {
        this(code, data, "");
    }

    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }

}
