package com.example.inventory.util;

import java.util.concurrent.CompletableFuture;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
/**
 * Utilidades para pruebas
 */
public class TestUtils {

  /**
   * Crea un WebClient mock configurado para simular respuestas exitosas
   */
  public static WebClient createMockWebClientWithSuccessResponse() {
    WebClient webClient = mock(WebClient.class);
    WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
    WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
    WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

    given(webClient.get()).willReturn(requestHeadersUriSpec);
    given(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).willReturn(requestHeadersSpec);
    given(requestHeadersSpec.headers(any())).willReturn(requestHeadersSpec);
    given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
    given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);

    return webClient;
  }

  /**
   * Crea un WebClient mock configurado para simular errores
   */
  public static WebClient createMockWebClientWithError() {
    WebClient webClient = mock(WebClient.class);
    WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
    WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
    WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

    given(webClient.get()).willReturn(requestHeadersUriSpec);
    given(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).willReturn(requestHeadersSpec);
    given(requestHeadersSpec.headers(any())).willReturn(requestHeadersSpec);
    given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
    given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
    given(responseSpec.bodyToMono(any(Class.class)))
        .willReturn(Mono.error(new RuntimeException("Connection error")));

    return webClient;
  }

  /**
   * Crea un KafkaTemplate mock para pruebas
   */
  @SuppressWarnings("unchecked")
  public static KafkaTemplate<String, Object> createMockKafkaTemplate() {
    KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
    SendResult<String, Object> sendResult = mock(SendResult.class);
    CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);

    given(kafkaTemplate.send(anyString(), anyString(), any())).willReturn(future);

    return kafkaTemplate;
  }

  /**
   * Crea un KafkaTemplate mock que simula fallos
   */
  @SuppressWarnings("unchecked")
  public static KafkaTemplate<String, Object> createMockKafkaTemplateWithError() {
    KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
    CompletableFuture<SendResult<String, Object>> failedFuture = new CompletableFuture<>();
    failedFuture.completeExceptionally(new RuntimeException("Kafka send failed"));

    given(kafkaTemplate.send(anyString(), anyString(), any())).willReturn(failedFuture);

    return kafkaTemplate;
  }
}