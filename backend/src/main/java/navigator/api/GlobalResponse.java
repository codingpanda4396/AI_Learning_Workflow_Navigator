package navigator.api;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 统一响应格式。失败时 data 为 null。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GlobalResponse<T> {

    private String code;
    private String message;
    private T data;

    public GlobalResponse() {
    }

    public GlobalResponse(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> GlobalResponse<T> ok(T data) {
        return new GlobalResponse<>(ResponseCode.OK.name(), "success", data);
    }

    public static <T> GlobalResponse<T> badRequest(String message) {
        return new GlobalResponse<>(ResponseCode.BAD_REQUEST.name(), message, null);
    }

    public static <T> GlobalResponse<T> notFound(String message) {
        return new GlobalResponse<>(ResponseCode.NOT_FOUND.name(), message, null);
    }

    public static <T> GlobalResponse<T> conflict(String message) {
        return new GlobalResponse<>(ResponseCode.CONFLICT.name(), message, null);
    }

    public static <T> GlobalResponse<T> internalError(String message) {
        return new GlobalResponse<>(ResponseCode.INTERNAL_ERROR.name(), message, null);
    }

    /**
     * Sprint 1: 业务错误响应，code 为 BusinessErrorCode 名称。
     */
    public static <T> GlobalResponse<T> businessError(BusinessErrorCode code, String message) {
        return new GlobalResponse<>(code.name(), message != null ? message : code.name(), null);
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
