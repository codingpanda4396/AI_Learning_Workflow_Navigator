package navigator.api;

/**
 * 业务异常，携带业务错误码，供 Guard 抛出并由 GlobalExceptionHandler 映射 HTTP 与 GlobalResponse。
 */
public class BusinessException extends RuntimeException {

    private final BusinessErrorCode code;

    public BusinessException(BusinessErrorCode code) {
        super(code.name());
        this.code = code;
    }

    public BusinessException(BusinessErrorCode code, String message) {
        super(message != null ? message : code.name());
        this.code = code;
    }

    public BusinessErrorCode getCode() {
        return code;
    }
}
