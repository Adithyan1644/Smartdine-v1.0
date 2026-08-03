package com.smartdine.coreheart;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.File;
import java.util.Map;
import java.util.Set;

@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Paths that produce expected / harmless 404s (WebSocket upgrade probes,
     * browser favicon requests, etc.) — do NOT log these to kds_error.log.
     */
    private static final Set<String> SILENT_404_PATHS = Set.of(
        "/ws/tunnel",
        "/favicon.ico",
        "/robots.txt"
    );

    /**
     * Handles NoResourceFoundException (HTTP 404) — thrown by Spring MVC when
     * a request targets a path that has no static resource or mapped handler.
     *
     * The /ws/tunnel endpoint is a @Profile("prod")-only WebSocket handler, so
     * on the local dev profile it does not exist and Spring logs it as an error.
     * We suppress those log entries here and return a clean 404 JSON response.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Object> handleNoResource(
            NoResourceFoundException ex,
            HttpServletRequest request) {

        String uri = request.getRequestURI();

        // Only log truly unexpected 404s — not WebSocket probes or browser noise
        boolean isSilent = SILENT_404_PATHS.stream().anyMatch(uri::startsWith)
                || uri.startsWith("/ws/");

        if (!isSilent) {
            System.err.println("[GlobalExceptionHandler] 404 Not Found: " + uri);
        }

        return new ResponseEntity<>(
            Map.of(
                "error", "Resource not found: " + uri,
                "status", "not_found",
                "path", uri
            ),
            HttpStatus.NOT_FOUND
        );
    }

    /**
     * Handles known, expected business logic exceptions that should NOT be
     * logged as critical errors:
     *   - "Invalid PIN"  — wrong staff PIN entered on Waiter / KDS app
     *   - "Invalid Username" / "Invalid Password" — admin login failures
     */
    private static final Set<String> SILENT_RUNTIME_MESSAGES = Set.of(
        "Invalid PIN",
        "Invalid Username",
        "Invalid Password"
    );

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Object> handleRuntimeException(
            RuntimeException ex,
            HttpServletRequest request) {

        String message = ex.getMessage();
        boolean isSilent = message != null && SILENT_RUNTIME_MESSAGES.contains(message.trim());

        if (!isSilent) {
            // Log unexpected runtime exceptions
            System.err.println("[GlobalExceptionHandler] RuntimeException: " + message);
        }

        return new ResponseEntity<>(
            Map.of(
                "error", message != null ? message : "Request failed",
                "status", "failed"
            ),
            HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAll(Exception ex, HttpServletRequest request) {
        System.err.println("[GlobalExceptionHandler] Exception: " + ex.getMessage());

        // Return detailed response
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        String stackTrace = sw.toString();
        if (stackTrace.length() > 800) {
            stackTrace = stackTrace.substring(0, 800) + "...";
        }

        return new ResponseEntity<>(
            Map.of(
                "error", ex.getMessage() != null ? ex.getMessage() : "Unknown error",
                "status", "failed",
                "type", ex.getClass().getName(),
                "stacktrace", stackTrace
            ),
            HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}