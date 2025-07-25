package com.example.inventory.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
/**
 * Configuración base para pruebas de integración
 */
@TestConfiguration
@Profile("integration-test")
class IntegrationTestConfiguration {

  /**
   * Configuración específica para pruebas de integración con TestContainers
   */
  @Bean
  public CacheManager integrationCacheManager() {
    return new ConcurrentMapCacheManager("product-info");
  }
}