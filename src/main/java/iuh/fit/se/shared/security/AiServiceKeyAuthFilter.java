package iuh.fit.se.shared.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.se.shared.config.AiProperties;
import iuh.fit.se.shared.response.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AiServiceKeyAuthFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AiServiceKeyAuthFilter.class);
    private static final String AI_SERVICE_KEY_HEADER = "X-AI-Service-Key";
    private static final String INTERNAL_AI_PREFIX = "/internal/ai";

    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;

    public AiServiceKeyAuthFilter(AiProperties aiProperties, ObjectMapper objectMapper) {
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestPath = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && requestPath.startsWith(contextPath)) {
            requestPath = requestPath.substring(contextPath.length());
        }

        return !(requestPath.equals(INTERNAL_AI_PREFIX) || requestPath.startsWith(INTERNAL_AI_PREFIX + "/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String configuredKey = aiProperties.getServiceKey();
        if (configuredKey == null || configuredKey.isBlank()) {
            LOGGER.warn("Rejected internal AI request because AI service key is not configured: method={}, path={}",
                    request.getMethod(),
                    request.getRequestURI());
            writeError(response, HttpStatus.UNAUTHORIZED, "Unauthorized");
            return;
        }

        String requestKey = request.getHeader(AI_SERVICE_KEY_HEADER);
        if (requestKey == null || requestKey.isBlank()) {
            LOGGER.warn("Rejected internal AI request due to missing service key: method={}, path={}, remote={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    request.getRemoteAddr());
            writeError(response, HttpStatus.UNAUTHORIZED, "Unauthorized");
            return;
        }

        if (!configuredKey.equals(requestKey)) {
            LOGGER.warn("Rejected internal AI request due to invalid service key: method={}, path={}, remote={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    request.getRemoteAddr());
            writeError(response, HttpStatus.FORBIDDEN, "Forbidden");
            return;
        }

        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                "ai-service",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_AI_SERVICE"))
        );
        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        LOGGER.info("Accepted internal AI request: method={}, path={}, remote={}",
                request.getMethod(),
                request.getRequestURI(),
                request.getRemoteAddr());

        filterChain.doFilter(request, response);
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(message)));
    }
}
