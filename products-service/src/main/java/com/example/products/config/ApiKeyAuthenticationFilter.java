package com.example.products.config;

import com.example.products.util.ApiKeyValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Filtro de autenticación por API Key
 */
@RequiredArgsConstructor
@Slf4j
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

  private final ApiKeyValidator apiKeyValidator;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

    String requestPath = request.getRequestURI();

    // Permitir endpoints públicos
    if (isPublicEndpoint(requestPath)) {
      filterChain.doFilter(request, response);
      return;
    }

    String apiKey = request.getHeader("X-API-Key");

    if (apiKey == null || apiKey.trim().isEmpty()) {
      handleAuthenticationError(response, "API Key faltante", "X-API-Key header es requerido");
      return;
    }

    if (!apiKeyValidator.isValidApiKey(apiKey)) {
      handleAuthenticationError(response, "API Key inválida", "La API Key proporcionada no es válida");
      return;
    }

    log.debug("API Key válida para request: {}", requestPath);
    filterChain.doFilter(request, response);
  }

  private boolean isPublicEndpoint(String path) {
    return path.startsWith("/actuator/") ||
        path.startsWith("/v3/api-docs") ||
        path.startsWith("/swagger-ui") ||
        path.equals("/swagger-ui.html");
  }

  private void handleAuthenticationError(HttpServletResponse response, String error, String message)
      throws IOException {

    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);

    Map<String, Object> errorResponse = new HashMap<>();
    errorResponse.put("success", false);
    errorResponse.put("message", error);
    errorResponse.put("details", message);
    errorResponse.put("timestamp", LocalDateTime.now().toString());

    response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
  }
}

