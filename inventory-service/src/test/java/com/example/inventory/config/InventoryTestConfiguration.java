package com.example.inventory.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Configuración de pruebas para el microservicio de inventario
 */
@TestConfiguration
@Profile("test")
@EnableCaching
public class InventoryTestConfiguration {

  /**
   * Cache manager para pruebas usando mapas en memoria
   */
  @Bean
  @Primary
  public CacheManager testCacheManager() {
    return new ConcurrentMapCacheManager("product-info");
  }

  /**
   * WebClient mock para pruebas
   */
  @Bean
  @Primary
  public WebClient testWebClient() {
    WebClient webClient = mock(WebClient.class);
    WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
    WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
    WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

    // Configuración básica del mock
    given(webClient.get()).willReturn(requestHeadersUriSpec);
    given(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).willReturn(requestHeadersSpec);
    given(requestHeadersSpec.headers(any())).willReturn(requestHeadersSpec);
    given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
    given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
    given(responseSpec.bodyToMono(any(Class.class))).willReturn(Mono.empty());

    return webClient;
  }

  /**
   * KafkaTemplate mock para pruebas
   */
  @Bean
  @Primary
  @SuppressWarnings("unchecked")
  public KafkaTemplate<String, Object> testKafkaTemplate() {
    KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
    SendResult<String, Object> sendResult = mock(SendResult.class);
    CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);

    given(kafkaTemplate.send(anyString(), anyString(), any())).willReturn(future);

    return kafkaTemplate;
  }
}