package com.example.inventory.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Configuración de WebClient para comunicación HTTP reactiva
 */
@Configuration
@Slf4j
public class WebClientConfig {

  @Value("${app.products-service.timeout.connect:5000}")
  private int connectTimeout;

  @Value("${app.products-service.timeout.read:10000}")
  private int readTimeout;

  @Value("${app.products-service.timeout.write:10000}")
  private int writeTimeout;

  @Value("${app.products-service.max-memory-size:1048576}") // 1MB default
  private int maxMemorySize;

  @Bean
  public WebClient webClient() {
    HttpClient httpClient = HttpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
        .responseTimeout(Duration.ofMillis(readTimeout))
        .doOnConnected(conn ->
            conn.addHandlerLast(new ReadTimeoutHandler(readTimeout, TimeUnit.MILLISECONDS))
                .addHandlerLast(new WriteTimeoutHandler(writeTimeout, TimeUnit.MILLISECONDS))
        );

    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(maxMemorySize))
        .filter(logRequest())
        .filter(logResponse())
        .filter(errorHandling())
        .build();
  }

  /**
   * Filtro para logging de requests
   */
  private ExchangeFilterFunction logRequest() {
    return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
      if (log.isDebugEnabled()) {
        log.debug("Request: {} {}", clientRequest.method(), clientRequest.url());
        clientRequest.headers().forEach((name, values) ->
            values.forEach(value -> {
              if (!name.equalsIgnoreCase("X-API-Key")) { // No logueamos API keys
                log.debug("Request Header: {}={}", name, value);
              }
            })
        );
      }
      return Mono.just(clientRequest);
    });
  }

  /**
   * Filtro para logging de responses
   */
  private ExchangeFilterFunction logResponse() {
    return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
      if (log.isDebugEnabled()) {
        log.debug("Response Status: {}", clientResponse.statusCode());
        clientResponse.headers().asHttpHeaders().forEach((name, values) ->
            values.forEach(value -> log.debug("Response Header: {}={}", name, value))
        );
      }
      return Mono.just(clientResponse);
    });
  }

  /**
   * Filtro para manejo de errores personalizado
   */
  private ExchangeFilterFunction errorHandling() {
    return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
      if (clientResponse.statusCode().isError()) {
        log.warn("HTTP Error: {} for request", clientResponse.statusCode());
      }
      return Mono.just(clientResponse);
    });
  }

  /**
   * WebClient específico para el servicio de productos con configuración personalizada
   */
  @Bean("productServiceWebClient")
  public WebClient productServiceWebClient(@Value("${app.products-service.base-url}") String baseUrl) {
    HttpClient httpClient = HttpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
        .responseTimeout(Duration.ofMillis(readTimeout))
        .doOnConnected(conn ->
            conn.addHandlerLast(new ReadTimeoutHandler(readTimeout, TimeUnit.MILLISECONDS))
                .addHandlerLast(new WriteTimeoutHandler(writeTimeout, TimeUnit.MILLISECONDS))
        );

    return WebClient.builder()
        .baseUrl(baseUrl)
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(maxMemorySize))
        .filter(logRequest())
        .filter(logResponse())
        .filter(errorHandling())
        .build();
  }
}