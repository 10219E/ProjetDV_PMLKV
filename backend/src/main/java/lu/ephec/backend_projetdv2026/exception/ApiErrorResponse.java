package lu.ephec.backend_projetdv2026.exception;

import java.time.LocalDateTime;

/**
 * Standard error payload returned by {@link GlobalExceptionHandler}.
 *
 * Keeping this strongly typed (instead of Map<String, Object>) helps Springdoc generate /v3/api-docs
 * reliably.
 */
public record ApiErrorResponse(
        LocalDateTime timestamp,
        int status,
        String message,
        String path
) {
}

