package com.example.products.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Validador de API Keys
 */
@Component
@Slf4j
public class ApiKeyValidator {

  @Value("${app.security.api-key}")
  private String validApiKey;

  /**
   * Valida si la API Key proporcionada es válida
   */
  public boolean isValidApiKey(String apiKey) {
    if (apiKey == null || apiKey.trim().isEmpty()) {
      log.warn("API Key is null or empty");
      return false;
    }

    boolean isValid = validApiKey.equals(apiKey.trim());

    if (!isValid) {
      log.warn("Invalid API Key provided: {}", apiKey.substring(0, Math.min(apiKey.length(), 10)) + "...");
    }

    return isValid;
  }
}
