package navigator.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 将异常统一转为 GlobalResponse 格式。
 * Sprint 1: BusinessException 按业务码映射 HTTP 404/400/409。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<GlobalResponse<Void>> handleBusiness(BusinessException ex) {
        BusinessErrorCode code = ex.getCode();
        HttpStatus status = httpStatusFor(code);
        return ResponseEntity.status(status)
                .body(GlobalResponse.businessError(code, ex.getMessage()));
    }

    private static HttpStatus httpStatusFor(BusinessErrorCode code) {
        switch (code) {
            case RESOURCE_NOT_FOUND:
                return HttpStatus.NOT_FOUND;
            case INVALID_ARGUMENT:
                return HttpStatus.BAD_REQUEST;
            case DIAGNOSIS_NOT_COMPLETED:
            case PLAN_NOT_COMMITTED:
            case TASK_NOT_CURRENT:
            case TASK_ALREADY_COMPLETED:
            case SESSION_NOT_COMPLETED:
            case SESSION_ALREADY_COMPLETED:
            case DIAGNOSIS_ALREADY_COMPLETED:
            case PLAN_ALREADY_COMMITTED:
                return HttpStatus.CONFLICT;
            default:
                return HttpStatus.BAD_REQUEST;
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GlobalResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .findFirst()
                .orElse("validation failed");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(GlobalResponse.badRequest(msg));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<GlobalResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(GlobalResponse.badRequest(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GlobalResponse<Void>> handleOther(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(GlobalResponse.internalError(ex.getMessage()));
    }
}
