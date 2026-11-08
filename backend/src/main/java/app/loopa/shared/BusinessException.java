package app.loopa.shared;

import org.springframework.http.HttpStatus;

/**
 * Erreur métier transformée en ProblemDetail (RFC 9457) par {@link app.loopa.shared.web.ApiExceptionHandler}.
 */
public class BusinessException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public BusinessException(String code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String code() {
        return code;
    }

    public HttpStatus status() {
        return status;
    }

    public static BusinessException notFound(String what, Object id) {
        return new BusinessException("NOT_FOUND", HttpStatus.NOT_FOUND, what + " introuvable : " + id);
    }
}
