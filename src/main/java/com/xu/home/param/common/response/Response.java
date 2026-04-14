package com.xu.home.param.common.response;

/**
 * 统一响应包装类
 *
 * @param <T> 数据类型
 */
public class Response<T> {
    private int code;
    private String codeMessage;
    private T data;

    public Response() {
    }

    public Response(ResponseStates responseStates, T data) {
        this.code = responseStates.getCode();
        this.codeMessage = responseStates.getMessage();
        this.data = data;
    }

    public Response(ResponseStates responseStates, T data, String codeMessage) {
        this.code = responseStates.getCode();
        this.codeMessage = codeMessage;
        this.data = data;
    }

    public Response(ResponseStates responseStates) {
        this.code = responseStates.getCode();
        this.codeMessage = responseStates.getMessage();
    }

    public Response(Integer code, T data, String message) {
        this.code = code;
        this.data = data;
        this.codeMessage = message;
    }

    public static <T> Response<T> success() {
        return new Response<>(ResponseStates.SUCCESS, null);
    }

    public static <T> Response<T> success(T data) {
        return new Response<>(ResponseStates.SUCCESS, data);
    }

    public static <T> Response<T> error(String codeMessage) {
        return new Response<>(ResponseStates.ERROR, null, codeMessage);
    }

    public static <T> Response<T> error(Integer code, String codeMessage) {
        return new Response<>(code, null, codeMessage);
    }

    public static <T> Response<T> error(ResponseStates responseStates, String codeMessage) {
        return new Response<>(responseStates.getCode(), null, codeMessage);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getCodeMessage() {
        return codeMessage;
    }

    public void setCodeMessage(String codeMessage) {
        this.codeMessage = codeMessage;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "Response{" +
                "code=" + code +
                ", codeMessage='" + codeMessage + '\'' +
                ", data=" + data +
                '}';
    }

    public static Response<?> checkResult(Integer result) {
        return new Response<>(result > 0 ? ResponseStates.SUCCESS : ResponseStates.OPERATIONFAILURE);
    }

    public static Response<?> checkResult(Boolean result) {
        return new Response<>(result ? ResponseStates.SUCCESS : ResponseStates.OPERATIONFAILURE);
    }
}
