package lu.ephec.backend_projetdv2026.exception;

import io.swagger.v3.oas.annotations.Hidden;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;

@RestControllerAdvice(basePackages = "lu.ephec.backend_projetdv2026.controller")
@Hidden
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatusException(ResponseStatusException ex, HttpServletRequest request) {
        int status = ex.getStatusCode().value();

        // We only expose the 'reason' message for controlled validation errors (400 Bad Request, 409 Conflict)
        // This prevents leaking sensitive information from internal server errors (500)
        boolean isValidationError = ex.getStatusCode() == HttpStatus.BAD_REQUEST || ex.getStatusCode() == HttpStatus.CONFLICT;

        String message = isValidationError ? ex.getReason() : "An unexpected error occurred";
        ApiErrorResponse body = new ApiErrorResponse(LocalDateTime.now(), status, message, request.getRequestURI());
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    // Optional: catch all other exceptions to ensure no internal messages leak
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleAllExceptions(Exception ex, HttpServletRequest request) {
        logger.error("[GlobalExceptionHandler] Unhandled exception on {}", request.getRequestURI(), ex);
        ApiErrorResponse body = new ApiErrorResponse(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected error occurred",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}

