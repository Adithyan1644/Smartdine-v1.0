package com.smartdine.coreheart;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.File;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAll(Exception ex, HttpServletRequest request) {
        try {
            // Write to a log file in the workspace
            File logFile = new File("C:\\Users\\ADITHYAN\\Desktop\\AVK\\smart_dine\\core-heart\\core-heart\\kds_error.log");
            try (FileWriter fw = new FileWriter(logFile, true);
                 PrintWriter pw = new PrintWriter(fw)) {
                pw.println("===============================================");
                pw.println("Time: " + java.time.LocalDateTime.now());
                pw.println("Request URI: " + request.getRequestURI());
                pw.println("HTTP Method: " + request.getMethod());
                pw.println("Exception: " + ex.getClass().getName());
                pw.println("Message: " + ex.getMessage());
                pw.println("Stack Trace:");
                ex.printStackTrace(pw);
                pw.println("===============================================");
                pw.println();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

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