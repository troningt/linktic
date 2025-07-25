package com.example.inventory.service.impl;

import com.example.inventory.controller.dto.ProductDto;
import com.example.inventory.service.ProductService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Implementación del servicio de comunicación con el microservicio de productos usando WebClient
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

  private final WebClient webClient;

  @Value("${app.products-service.base-url}")
  private String productsServiceBaseUrl;

  @Value("${app.products-service.api-key}")
  private String apiKey;

  @Value("${app.products-service.timeout.connect:5000}")
  private int connectTimeout;

  @Value("${app.products-service.timeout.read:10000}")
  private int readTimeout;

  @Override
  @CircuitBreaker(name = "productService", fallbackMethod = "getProductByIdFallback")
  @Retry(name = "productService")
  @TimeLimiter(name = "productService")
  @Cacheable(value = "product-info", key = "#productId", unless = "#result == null || #result.isEmpty()")
  public Optional<ProductDto> getProductById(Long productId) {
    log.debug("Fetching product information for ID: {}", productId);

    try {
      ProductDto product = webClient
          .get()
          .uri(productsServiceBaseUrl + "/api/v1/products/{productId}", productId)
          .headers(headers -> {
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.set("X-API-Key", apiKey);
            headers.set("User-Agent", "inventory-service/1.0.0");
          })
          .retrieve()
          .onStatus(HttpStatusCode::is4xxClientError, response -> {
            if (response.statusCode() == HttpStatus.NOT_FOUND) {
              log.warn("Product not found for ID: {}", productId);
              return Mono.empty(); // No error, just empty result
            }
            return Mono.error(new RuntimeException("Client error: " + response.statusCode()));
          })
          .onStatus(HttpStatusCode::is5xxServerError, response ->
              Mono.error(new RuntimeException("Server error: " + response.statusCode())))
          .bodyToMono(ProductDto.class)
          .timeout(Duration.ofMillis(readTimeout))
          .block();

      if (product != null) {
        log.debug("Successfully fetched product: {}", product.getName());
        return Optional.of(product);
      } else {
        log.warn("Product not found for ID: {}", productId);
        return Optional.empty();
      }

    } catch (WebClientResponseException.NotFound e) {
      log.warn("Product not found for ID: {}", productId);
      return Optional.empty();
    } catch (Exception e) {
      log.error("Error communicating with products service for product ID: {}", productId, e);
      throw e; // Re-throw para que el circuit breaker y retry funcionen
    }
  }

  @Override
  @CircuitBreaker(name = "productService", fallbackMethod = "isProductActiveByIdFallback")
  @io.github.resilience4j.retry.annotation.Retry(name = "productService")
  public boolean isProductActiveById(Long productId) {
    log.debug("Checking if product is active for ID: {}", productId);

    Optional<ProductDto> productOpt = getProductById(productId);
    return productOpt.map(ProductDto::getActive).orElse(false);
  }

  @Override
  @CircuitBreaker(name = "productService", fallbackMethod = "getProductInfoSafelyFallback")
  public ProductDto getProductInfoSafely(Long productId) {
    log.debug("Safely fetching product information for ID: {}", productId);

    try {
      Optional<ProductDto> productOpt = getProductById(productId);
      return productOpt.orElse(createFallbackProduct(productId));
    } catch (Exception e) {
      log.warn("Failed to fetch product info safely for ID: {}, using fallback", productId, e);
      return createFallbackProduct(productId);
    }
  }

  @Override
  @CircuitBreaker(name = "productService", fallbackMethod = "isProductServiceAvailableFallback")
  public boolean isProductServiceAvailable() {
    log.debug("Checking product service availability");

    try {
      String response = webClient
          .get()
          .uri(productsServiceBaseUrl + "/api/v1/actuator/health")
          .headers(headers -> {
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-API-Key", apiKey);
            headers.set("User-Agent", "inventory-service/1.0.0");
          })
          .retrieve()
          .onStatus(HttpStatusCode::isError, clientResponse ->
              Mono.error(new RuntimeException("Health check failed")))
          .bodyToMono(String.class)
          .timeout(Duration.ofMillis(connectTimeout))
          .block();

      boolean available = response != null;
      log.debug("Product service availability: {}", available);
      return available;

    } catch (Exception e) {
      log.warn("Product service is not available: {}", e.getMessage());
      return false;
    }
  }

  @Override
  public Optional<ProductDto> getProductWithRetry(Long productId, int maxRetries) {
    log.debug("Fetching product with retry for ID: {}, max retries: {}", productId, maxRetries);

    try {
      ProductDto product = webClient
          .get()
          .uri(productsServiceBaseUrl + "/api/v1/products/{productId}", productId)
          .headers(headers -> {
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.set("X-API-Key", apiKey);
            headers.set("User-Agent", "inventory-service/1.0.0");
          })
          .retrieve()
          .onStatus(HttpStatusCode::is4xxClientError, response -> {
            if (response.statusCode() == HttpStatus.NOT_FOUND) {
              return Mono.empty();
            }
            return Mono.error(new RuntimeException("Client error: " + response.statusCode()));
          })
          .bodyToMono(ProductDto.class)
          .retryWhen(reactor.util.retry.Retry.backoff(maxRetries, Duration.ofSeconds(1))
              .maxBackoff(Duration.ofSeconds(10))
              .jitter(0.5)
              .doBeforeRetry(retrySignal ->
                  log.warn("Retrying request for product ID: {}, attempt: {}",
                      productId, retrySignal.totalRetries() + 1)))
          .timeout(Duration.ofMillis(readTimeout))
          .block();

      if (product != null) {
        log.debug("Successfully fetched product with retry");
        return Optional.of(product);
      } else {
        return Optional.empty();
      }

    } catch (Exception e) {
      log.error("Failed to fetch product ID: {} after {} attempts", productId, maxRetries + 1, e);
      return Optional.empty();
    }
  }

  // ==================== MÉTODOS ASÍNCRONOS ====================

  /**
   * Versión asíncrona de getProductById
   */
  public CompletableFuture<Optional<ProductDto>> getProductByIdAsync(Long productId) {
    log.debug("Fetching product information asynchronously for ID: {}", productId);

    return webClient
        .get()
        .uri(productsServiceBaseUrl + "/api/v1/products/{productId}", productId)
        .headers(headers -> {
          headers.setContentType(MediaType.APPLICATION_JSON);
          headers.setAccept(List.of(MediaType.APPLICATION_JSON));
          headers.set("X-API-Key", apiKey);
          headers.set("User-Agent", "inventory-service/1.0.0");
        })
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, response -> {
          if (response.statusCode() == HttpStatus.NOT_FOUND) {
            return Mono.empty();
          }
          return Mono.error(new RuntimeException("Client error: " + response.statusCode()));
        })
        .bodyToMono(ProductDto.class)
        .timeout(Duration.ofMillis(readTimeout))
        .map(Optional::of)
        .onErrorReturn(Optional.empty())
        .toFuture();
  }

  // ==================== MÉTODOS FALLBACK ====================

  /**
   * Fallback method para getProductById
   */
  public Optional<ProductDto> getProductByIdFallback(Long productId, Exception ex) {
    log.warn("Circuit breaker activated for getProductById with product ID: {}", productId, ex);
    return Optional.of(createFallbackProduct(productId));
  }

  /**
   * Fallback method para isProductActiveById
   */
  public boolean isProductActiveByIdFallback(Long productId, Exception ex) {
    log.warn("Circuit breaker activated for isProductActiveById with product ID: {}", productId, ex);
    return true; // Asumimos activo para no bloquear operaciones
  }

  /**
   * Fallback method para getProductInfoSafely
   */
  public ProductDto getProductInfoSafelyFallback(Long productId, Exception ex) {
    log.warn("Circuit breaker activated for getProductInfoSafely with product ID: {}", productId, ex);
    return createFallbackProduct(productId);
  }

  /**
   * Fallback method para isProductServiceAvailable
   */
  public boolean isProductServiceAvailableFallback(Exception ex) {
    log.warn("Circuit breaker activated for isProductServiceAvailable", ex);
    return false;
  }

  // ==================== MÉTODOS AUXILIARES ====================

  /**
   * Crea un producto fallback con información básica
   */
  private ProductDto createFallbackProduct(Long productId) {
    return ProductDto.builder()
        .id(productId)
        .name("Product #" + productId)
        .price(java.math.BigDecimal.ZERO)
        .description("Product information temporarily unavailable")
        .active(true)
        .build();
  }

  /**
   * Método para probar la conectividad con timeout personalizado
   */
  public CompletableFuture<Boolean> testConnectivityAsync() {
    return webClient
        .get()
        .uri(productsServiceBaseUrl + "/api/v1/actuator/health")
        .headers(headers -> {
          headers.set("X-API-Key", apiKey);
          headers.set("User-Agent", "inventory-service/1.0.0");
        })
        .retrieve()
        .bodyToMono(String.class)
        .timeout(Duration.ofSeconds(5))
        .map(response -> true)
        .onErrorReturn(false)
        .toFuture();
  }

  /**
   * Obtiene información de múltiples productos en batch usando programación reactiva
   */
  public CompletableFuture<Map<Long, ProductDto>> getProductsBatchAsync(List<Long> productIds) {
    log.debug("Fetching products in batch asynchronously: {}", productIds);

    return reactor.core.publisher.Flux.fromIterable(productIds)
        .flatMap(productId ->
            webClient
                .get()
                .uri(productsServiceBaseUrl + "/api/v1/products/{productId}", productId)
                .headers(headers -> {
                  headers.setContentType(MediaType.APPLICATION_JSON);
                  headers.setAccept(List.of(MediaType.APPLICATION_JSON));
                  headers.set("X-API-Key", apiKey);
                  headers.set("User-Agent", "inventory-service/1.0.0");
                })
                .retrieve()
                .bodyToMono(ProductDto.class)
                .timeout(Duration.ofMillis(readTimeout))
                .onErrorReturn(createFallbackProduct(productId))
                .map(product -> Map.entry(productId, product))
        )
        .collectMap(Map.Entry::getKey, Map.Entry::getValue)
        .toFuture();
  }

  /**
   * Versión síncrona del batch para compatibilidad
   */
  public Map<Long, ProductDto> getProductsBatch(List<Long> productIds) {
    log.debug("Fetching products in batch: {}", productIds);

    try {
      return getProductsBatchAsync(productIds)
          .get(readTimeout * productIds.size(), TimeUnit.MILLISECONDS);
    } catch (Exception e) {
      log.error("Failed to fetch products in batch", e);
      Map<Long, ProductDto> fallbackResults = new HashMap<>();
      productIds.forEach(id -> fallbackResults.put(id, createFallbackProduct(id)));
      return fallbackResults;
    }
  }

  /**
   * Invalida la cache para un producto específico
   */
  @CacheEvict(value = "product-info", key = "#productId")
  public void evictProductCache(Long productId) {
    log.debug("Evicting cache for product ID: {}", productId);
  }

  /**
   * Invalida toda la cache de productos
   */
  @CacheEvict(value = "product-info", allEntries = true)
  public void evictAllProductCache() {
    log.info("Evicting all product cache entries");
  }
}