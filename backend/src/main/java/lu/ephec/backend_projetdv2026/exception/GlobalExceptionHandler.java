package lu.ephec.backend_projetdv2026.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", ex.getStatusCode().value());

        // We only expose the 'reason' message for controlled validation errors (400 Bad Request, 409 Conflict)
        // This prevents leaking sensitive information from internal server errors (500)
        boolean isValidationError = ex.getStatusCode() == HttpStatus.BAD_REQUEST || ex.getStatusCode() == HttpStatus.CONFLICT;

        if (isValidationError) {
            body.put("message", ex.getReason());
        } else {
            body.put("message", "An unexpected error occurred");
        }

        return new ResponseEntity<>(body, ex.getStatusCode());
    }

    // Optional: catch all other exceptions to ensure no internal messages leak
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAllExceptions(Exception ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("message", "An unexpected error occurred");

        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

