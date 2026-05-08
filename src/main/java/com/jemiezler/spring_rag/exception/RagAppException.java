package com.jemiezler.spring_rag.exception;

import org.springframework.http.HttpStatus;

/**
 * Base application exception. Carries an HTTP status so the global handler
 * can map it directly without needing a separate exception hierarchy per status code.
 */
public class RagAppException extends RuntimeException {

    private final HttpStatus status;

    public RagAppException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public RagAppException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    // ---- Convenience factory methods ----

    public static RagAppException badRequest(String message) {
        return new RagAppException(message, HttpStatus.BAD_REQUEST);
    }

    public static RagAppException internalError(String message, Throwable cause) {
        return new RagAppException(message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}