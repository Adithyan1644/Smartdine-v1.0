package com.smartdine.coreheart;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalErrorFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            chain.doFilter(request, response);
        } catch (Throwable ex) {
            handleException(ex, response);
            return;
        }

        // Check if Tomcat/Spring Boot routed to /error and stored the exception
        Throwable ex = (Throwable) request.getAttribute("jakarta.servlet.error.exception");
        if (ex == null) {
            ex = (Throwable) request.getAttribute("org.springframework.boot.web.servlet.error.DefaultErrorAttributes.ERROR");
        }
        if (ex != null) {
            handleException(ex, response);
        }
    }

    private void handleException(Throwable ex, ServletResponse response) throws IOException {
        System.err.println("[GLOBAL ERROR FILTER] Caught exception: " + ex.getMessage());
        ex.printStackTrace();

        HttpServletResponse httpResponse = (HttpServletResponse) response;
        if (!httpResponse.isCommitted()) {
            try {
                httpResponse.resetBuffer();
            } catch (Exception e) {
                // Ignore if unable to reset buffer
            }
            httpResponse.setStatus(500);
            httpResponse.setContentType("application/json");
            
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            String stackTrace = sw.toString();
            if (stackTrace.length() > 800) {
                stackTrace = stackTrace.substring(0, 800) + "...";
            }

            // Escape characters for JSON
            String safeMessage = ex.getMessage() != null ? ex.getMessage() : "Unknown error";
            safeMessage = safeMessage.replace("\"", "\\\"").replace("\n", " ").replace("\r", "");
            
            String safeStackTrace = stackTrace.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");

            String json = String.format(
                "{\"status\":\"failed\",\"error\":\"%s\",\"type\":\"%s\",\"stacktrace\":\"%s\"}",
                safeMessage,
                ex.getClass().getName(),
                safeStackTrace
            );
            httpResponse.getWriter().write(json);
        }
    }
}
