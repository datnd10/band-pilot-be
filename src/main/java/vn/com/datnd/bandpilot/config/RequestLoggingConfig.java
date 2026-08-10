package vn.com.datnd.bandpilot.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

/**
 * Logs every incoming HTTP request with method, URI, status, and duration.
 * Health-check pings are logged at DEBUG to avoid log noise.
 */
@Component
public class RequestLoggingConfig extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingConfig.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        Instant start = Instant.now();
        String method = request.getMethod();
        String uri = request.getRequestURI();

        try {
            chain.doFilter(request, response);
        } finally {
            long ms = Duration.between(start, Instant.now()).toMillis();
            int status = response.getStatus();

            // Log health checks at DEBUG to reduce noise
            if (uri.contains("/health")) {
                log.debug("{} {} → {} ({}ms)", method, uri, status, ms);
            } else {
                log.info("{} {} → {} ({}ms)", method, uri, status, ms);
            }
        }
    }
}
