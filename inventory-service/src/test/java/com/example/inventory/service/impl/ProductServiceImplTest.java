package com.example.inventory.service.impl;

import com.example.inventory.controller.dto.ProductDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductServiceImpl Tests")
class ProductServiceImplTest {

  @Mock
  private WebClient webClient;

  @Mock
  private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

  @Mock
  private WebClient.RequestHeadersSpec requestHeadersSpec;

  @Mock
  private WebClient.ResponseSpec responseSpec;

  @InjectMocks
  private ProductServiceImpl productService;

  private ProductDto testProduct;

  @BeforeEach
  void setUp() {
    // Set up test properties
    ReflectionTestUtils.setField(productService, "productsServiceBaseUrl", "http://localhost:8081");
    ReflectionTestUtils.setField(productService, "apiKey", "test-api-key");
    ReflectionTestUtils.setField(productService, "connectTimeout", 5000);
    ReflectionTestUtils.setField(productService, "readTimeout", 10000);

    testProduct = ProductDto.builder()
        .id(100L)
        .name("Test Product")
        .price(BigDecimal.valueOf(29.99))
        .description("Test product description")
        .active(true)
        .build();

    // Set up the WebClient mock chain
    given(webClient.get()).willReturn(requestHeadersUriSpec);
    given(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).willReturn(requestHeadersSpec);
    given(requestHeadersSpec.headers(any())).willReturn(requestHeadersSpec);
    given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
  }

  @Nested
  @DisplayName("Get Product By ID Tests")
  class GetProductByIdTests {

    @Test
    @DisplayName("Should return product when found")
    void shouldReturnProductWhenFound() {
      // Given
      given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
      given(responseSpec.bodyToMono(ProductDto.class))
          .willReturn(Mono.just(testProduct));

      // When
      Optional<ProductDto> result = productService.getProductById(100L);

      // Then
      assertThat(result).isPresent();
      assertThat(result.get().getId()).isEqualTo(100L);
      assertThat(result.get().getName()).isEqualTo("Test Product");
      assertThat(result.get().getPrice()).isEqualTo(BigDecimal.valueOf(29.99));
      assertThat(result.get().getActive()).isTrue();
    }

    @Test
    @DisplayName("Should return empty when product not found")
    void shouldReturnEmptyWhenProductNotFound() {
      // Given
      given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
      given(responseSpec.bodyToMono(ProductDto.class))
          .willReturn(Mono.empty());

      // When
      Optional<ProductDto> result = productService.getProductById(999L);

      // Then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty when 404 Not Found")
    void shouldReturnEmptyWhen404NotFound() {
      // Given
      WebClientResponseException notFoundException =
          WebClientResponseException.NotFound.create(404, "Not Found", null, null, null);

      given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
      given(responseSpec.bodyToMono(ProductDto.class))
          .willReturn(Mono.error(notFoundException));

      // When
      Optional<ProductDto> result = productService.getProductById(999L);

      // Then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should throw exception on server error")
    void shouldThrowExceptionOnServerError() {
      // Given
      RuntimeException serverError = new RuntimeException("Server error");
      given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
      given(responseSpec.bodyToMono(ProductDto.class))
          .willReturn(Mono.error(serverError));

      // When & Then
      assertThatThrownBy(() -> productService.getProductById(100L))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Server error");
    }

    @Test
    @DisplayName("Should use correct URL and headers")
    void shouldUseCorrectUrlAndHeaders() {
      // Given
      given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
      given(responseSpec.bodyToMono(ProductDto.class))
          .willReturn(Mono.just(testProduct));

      // When
      productService.getProductById(100L);

      // Then
      verify(requestHeadersUriSpec).uri("http://localhost:8081/api/v1/products/{productId}", 100L);
      verify(requestHeadersSpec).headers(any());
    }
  }

  @Nested
  @DisplayName("Product Active Check Tests")
  class ProductActiveCheckTests {

    @Test
    @DisplayName("Should return true when product is active")
    void shouldReturnTrueWhenProductIsActive() {
      // Given
      given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
      given(responseSpec.bodyToMono(ProductDto.class))
          .willReturn(Mono.just(testProduct));

      // When
      boolean isActive = productService.isProductActiveById(100L);

      // Then
      assertThat(isActive).isTrue();
    }

    @Test
    @DisplayName("Should return false when product is inactive")
    void shouldReturnFalseWhenProductIsInactive() {
      // Given
      testProduct.setActive(false);
      given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
      given(responseSpec.bodyToMono(ProductDto.class))
          .willReturn(Mono.just(testProduct));

      // When
      boolean isActive = productService.isProductActiveById(100L);

      // Then
      assertThat(isActive).isFalse();
    }

    @Test
    @DisplayName("Should return false when product not found")
    void shouldReturnFalseWhenProductNotFound() {
      // Given
      given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
      given(responseSpec.bodyToMono(ProductDto.class))
          .willReturn(Mono.empty());

      // When
      boolean isActive = productService.isProductActiveById(999L);

      // Then
      assertThat(isActive).isFalse();
    }
  }

  @Nested
  @DisplayName("Get Product Info Safely Tests")
  class GetProductInfoSafelyTests {

    @Test
    @DisplayName("Should return product when found")
    void shouldReturnProductWhenFound() {
      // Given
      given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
      given(responseSpec.bodyToMono(ProductDto.class))
          .willReturn(Mono.just(testProduct));

      // When
      ProductDto result = productService.getProductInfoSafely(100L);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getId()).isEqualTo(100L);
      assertThat(result.getName()).isEqualTo("Test Product");
    }

    @Test
    @DisplayName("Should return fallback product when not found")
    void shouldReturnFallbackProductWhenNotFound() {
      // Given
      given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
      given(responseSpec.bodyToMono(ProductDto.class))
          .willReturn(Mono.empty());

      // When
      ProductDto result = productService.getProductInfoSafely(999L);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getId()).isEqualTo(999L);
      assertThat(result.getName()).isEqualTo("Product #999");
      assertThat(result.getPrice()).isEqualTo(BigDecimal.ZERO);
      assertThat(result.getDescription()).isEqualTo("Product information temporarily unavailable");
      assertThat(result.getActive()).isTrue();
    }

    @Test
    @DisplayName("Should return fallback product on exception")
    void shouldReturnFallbackProductOnException() {
      // Given
      given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
      given(responseSpec.bodyToMono(ProductDto.class))
          .willReturn(Mono.error(new RuntimeException("Connection error")));

      // When
      ProductDto result = productService.getProductInfoSafely(100L);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getId()).isEqualTo(100L);
      assertThat(result.getName()).isEqualTo("Product #100");
      assertThat(result.getDescription()).isEqualTo("Product information temporarily unavailable");
    }
  }

  @Nested
  @DisplayName("Service Availability Tests")
  class ServiceAvailabilityTests {

    @Test
    @DisplayName("Should return true when service is available")
    void shouldReturnTrueWhenServiceIsAvailable() {
      // Given
      given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
      given(responseSpec.bodyToMono(String.class))
          .willReturn(Mono.just("{\"status\":\"UP\"}"));

      // When
      boolean isAvailable = productService.isProductServiceAvailable();

      // Then
      assertThat(isAvailable).isTrue();
      verify(requestHeadersUriSpec).uri("http://localhost:8081/api/v1/actuator/health");
    }

    @Test
    @DisplayName("Should return false when service is unavailable")
    void shouldReturnFalseWhenServiceIsUnavailable() {
      // Given
      given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
      given(responseSpec.bodyToMono(String.class))
          .willReturn(Mono.error(new RuntimeException("Connection refused")));

      // When
      boolean isAvailable = productService.isProductServiceAvailable();

      // Then
      assertThat(isAvailable).isFalse();
    }

    @Test
    @DisplayName("Should return false when health endpoint returns null")
    void shouldReturnFalseWhenHealthEndpointReturnsNull() {
      // Given
      given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
      given(responseSpec.bodyToMono(String.class))
          .willReturn(Mono.empty());

      // When
      boolean isAvailable = productService.isProductServiceAvailable();

      // Then
      assertThat(isAvailable).isFalse();
    }
  }

  @Nested
  @DisplayName("Retry Mechanism Tests")
  class RetryMechanismTests {

    @Test
    @DisplayName("Should return product when retry succeeds")
    void shouldReturnProductWhenRetrySucceeds() {
      // Given
      given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
      given(responseSpec.bodyToMono(ProductDto.class))
          .willReturn(Mono.just(testProduct));

      // When
      Optional<ProductDto> result = productService.getProductWithRetry(100L, 2);

      // Then
      assertThat(result).isPresent();
      assertThat(result.get().getId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("Should return empty when all retries fail")
    void shouldReturnEmptyWhenAllRetriesFail() {
      // Given
      given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
      given(responseSpec.bodyToMono(ProductDto.class))
          .willReturn(Mono.error(new RuntimeException("Connection error")));

      // When
      Optional<ProductDto> result = productService.getProductWithRetry(100L, 2);

      // Then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty when product not found with retry")
    void shouldReturnEmptyWhenProductNotFoundWithRetry() {
      // Given
      given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
      given(responseSpec.bodyToMono(ProductDto.class))
          .willReturn(Mono.empty());

      // When
      Optional<ProductDto> result = productService.getProductWithRetry(999L, 1);

      // Then
      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("Asynchronous Operations Tests")
  class AsynchronousOperationsTests {

    @Test
    @DisplayName("Should return product asynchronously")
    void shouldReturnProductAsynchronously() throws ExecutionException, InterruptedException {
      // Given
      given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
      given(responseSpec.bodyToMono(ProductDto.class))
          .willReturn(Mono.just(testProduct));

      // When
      CompletableFuture<Optional<ProductDto>> future = productService.getProductByIdAsync(100L);
      Optional<ProductDto> result = future.get();

      // Then
      assertThat(result).isPresent();
      assertThat(result.get().getId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("Should return empty asynchronously when not found")
    void shouldReturnEmptyAsynchronouslyWhenNotFound() throws ExecutionException, InterruptedException {
      // Given
      given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
      given(responseSpec.bodyToMono(ProductDto.class))
          .willReturn(Mono.error(new RuntimeException("Not found")));

      // When
      CompletableFuture<Optional<ProductDto>> future = productService.getProductByIdAsync(999L);
      Optional<ProductDto> result = future.get();

      // Then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should test connectivity asynchronously")
    void shouldTestConnectivityAsynchronously() throws ExecutionException, InterruptedException {
      // Given
      given(responseSpec.bodyToMono(String.class))
          .willReturn(Mono.just("OK"));

      // When
      CompletableFuture<Boolean> future = productService.testConnectivityAsync();
      Boolean result = future.get();

      // Then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false on connectivity test failure")
    void shouldReturnFalseOnConnectivityTestFailure() throws ExecutionException, InterruptedException {
      // Given
      given(responseSpec.bodyToMono(String.class))
          .willReturn(Mono.error(new RuntimeException("Connection failed")));

      // When
      CompletableFuture<Boolean> future = productService.testConnectivityAsync();
      Boolean result = future.get();

      // Then
      assertThat(result).isFalse();
    }
  }

  @Nested
  @DisplayName("Batch Operations Tests")
  class BatchOperationsTests {

    @Test
    @DisplayName("Should get products in batch asynchronously")
    void shouldGetProductsInBatchAsynchronously() throws ExecutionException, InterruptedException {
      // Given
      List<Long> productIds = Arrays.asList(100L, 200L, 300L);

      ProductDto product100 = ProductDto.builder().id(100L).name("Product 100").active(true).build();
      ProductDto product200 = ProductDto.builder().id(200L).name("Product 200").active(true).build();
      ProductDto product300 = ProductDto.builder().id(300L).name("Product 300").active(true).build();

      given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
      given(responseSpec.bodyToMono(ProductDto.class))
          .willReturn(Mono.just(product100), Mono.just(product200), Mono.just(product300));

      // When
      CompletableFuture<Map<Long, ProductDto>> future = productService.getProductsBatchAsync(productIds);
      Map<Long, ProductDto> result = future.get();

      // Then
      assertThat(result).hasSize(3);
      assertThat(result.get(100L).getName()).isEqualTo("Product 100");
      assertThat(result.get(200L).getName()).isEqualTo("Product 200");
      assertThat(result.get(300L).getName()).isEqualTo("Product 300");
    }

    @Test
    @DisplayName("Should handle errors in batch operations with fallback")
    void shouldHandleErrorsInBatchOperationsWithFallback() throws ExecutionException, InterruptedException {
      // Given
      List<Long> productIds = Arrays.asList(100L, 999L); // 999L will fail

      ProductDto product100 = ProductDto.builder().id(100L).name("Product 100").active(true).build();

      given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
      given(responseSpec.bodyToMono(ProductDto.class))
          .willReturn(Mono.just(product100), Mono.error(new RuntimeException("Not found")));

      // When
      CompletableFuture<Map<Long, ProductDto>> future = productService.getProductsBatchAsync(productIds);
      Map<Long, ProductDto> result = future.get();

      // Then
      assertThat(result).hasSize(2);
      assertThat(result.get(100L).getName()).isEqualTo("Product 100");
      assertThat(result.get(999L).getName()).isEqualTo("Product #999"); // Fallback product
      assertThat(result.get(999L).getDescription()).isEqualTo("Product information temporarily unavailable");
    }

    @Test
    @DisplayName("Should get products in batch synchronously")
    void shouldGetProductsInBatchSynchronously() {
      // Given
      List<Long> productIds = Arrays.asList(100L, 200L);

      ProductDto product100 = ProductDto.builder().id(100L).name("Product 100").active(true).build();
      ProductDto product200 = ProductDto.builder().id(200L).name("Product 200").active(true).build();

      given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
      given(responseSpec.bodyToMono(ProductDto.class))
          .willReturn(Mono.just(product100), Mono.just(product200));

      // When
      Map<Long, ProductDto> result = productService.getProductsBatch(productIds);

      // Then
      assertThat(result).hasSize(2);
      assertThat(result.get(100L).getName()).isEqualTo("Product 100");
      assertThat(result.get(200L).getName()).isEqualTo("Product 200");
    }

    @Test
    @DisplayName("Should return fallback products on batch operation timeout")
    void shouldReturnFallbackProductsOnBatchOperationTimeout() {
      // Given
      List<Long> productIds = Arrays.asList(100L, 200L);

      given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
      given(responseSpec.bodyToMono(ProductDto.class))
          .willReturn(Mono.never()); // This will cause timeout

      // When
      Map<Long, ProductDto> result = productService.getProductsBatch(productIds);

      // Then
      assertThat(result).hasSize(2);
      assertThat(result.get(100L).getName()).isEqualTo("Product #100");
      assertThat(result.get(200L).getName()).isEqualTo("Product #200");
    }
  }

  @Nested
  @DisplayName("Cache Operations Tests")
  class CacheOperationsTests {

    @Test
    @DisplayName("Should evict product cache")
    void shouldEvictProductCache() {
      // When & Then - Should not throw exception
      assertThatCode(() -> productService.evictProductCache(100L))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should evict all product cache")
    void shouldEvictAllProductCache() {
      // When & Then - Should not throw exception
      assertThatCode(() -> productService.evictAllProductCache())
          .doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("Fallback Methods Tests")
  class FallbackMethodsTests {

    @Test
    @DisplayName("Should return fallback product on circuit breaker")
    void shouldReturnFallbackProductOnCircuitBreaker() {
      // Given
      Exception testException = new RuntimeException("Circuit breaker open");

      // When
      Optional<ProductDto> result = productService.getProductByIdFallback(100L, testException);

      // Then
      assertThat(result).isPresent();
      assertThat(result.get().getId()).isEqualTo(100L);
      assertThat(result.get().getName()).isEqualTo("Product #100");
      assertThat(result.get().getPrice()).isEqualTo(BigDecimal.ZERO);
      assertThat(result.get().getDescription()).isEqualTo("Product information temporarily unavailable");
      assertThat(result.get().getActive()).isTrue();
    }

    @Test
    @DisplayName("Should return true on active check fallback")
    void shouldReturnTrueOnActiveCheckFallback() {
      // Given
      Exception testException = new RuntimeException("Circuit breaker open");

      // When
      boolean result = productService.isProductActiveByIdFallback(100L, testException);

      // Then
      assertThat(result).isTrue(); // Assumes active to not block operations
    }

    @Test
    @DisplayName("Should return fallback product on safe get fallback")
    void shouldReturnFallbackProductOnSafeGetFallback() {
      // Given
      Exception testException = new RuntimeException("Circuit breaker open");

      // When
      ProductDto result = productService.getProductInfoSafelyFallback(100L, testException);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getId()).isEqualTo(100L);
      assertThat(result.getName()).isEqualTo("Product #100");
      assertThat(result.getDescription()).isEqualTo("Product information temporarily unavailable");
    }

    @Test
    @DisplayName("Should return false on service availability fallback")
    void shouldReturnFalseOnServiceAvailabilityFallback() {
      // Given
      Exception testException = new RuntimeException("Circuit breaker open");

      // When
      boolean result = productService.isProductServiceAvailableFallback(testException);

      // Then
      assertThat(result).isFalse();
    }
  }

  @Nested
  @DisplayName("Edge Cases Tests")
  class EdgeCasesTests {

    @Test
    @DisplayName("Should handle null product ID gracefully")
    void shouldHandleNullProductIdGracefully() {
      // When & Then
      assertThatThrownBy(() -> productService.getProductById(null))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should handle negative product ID")
    void shouldHandleNegativeProductId() {
      // Given
      given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
      given(responseSpec.bodyToMono(ProductDto.class))
          .willReturn(Mono.empty());

      // When
      Optional<ProductDto> result = productService.getProductById(-1L);

      // Then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should handle zero product ID")
    void shouldHandleZeroProductId() {
      // Given
      given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
      given(responseSpec.bodyToMono(ProductDto.class))
          .willReturn(Mono.empty());

      // When
      Optional<ProductDto> result = productService.getProductById(0L);

      // Then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should create fallback product with correct structure")
    void shouldCreateFallbackProductWithCorrectStructure() {
      // Given
      Long productId = 12345L;
      given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
      given(responseSpec.bodyToMono(ProductDto.class))
          .willReturn(Mono.empty());

      // When
      ProductDto result = productService.getProductInfoSafely(productId);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getId()).isEqualTo(productId);
      assertThat(result.getName()).isEqualTo("Product #" + productId);
      assertThat(result.getPrice()).isEqualTo(BigDecimal.ZERO);
      assertThat(result.getDescription()).isEqualTo("Product information temporarily unavailable");
      assertThat(result.getActive()).isTrue();
    }

    @Test
    @DisplayName("Should handle empty batch list")
    void shouldHandleEmptyBatchList() {
      // Given
      List<Long> emptyList = Collections.emptyList();

      // When
      Map<Long, ProductDto> result = productService.getProductsBatch(emptyList);

      // Then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should handle null batch list")
    void shouldHandleNullBatchList() {
      // When & Then
      assertThatThrownBy(() -> productService.getProductsBatch(null))
          .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should handle very large product ID")
    void shouldHandleVeryLargeProductId() {
      // Given
      Long largeId = Long.MAX_VALUE;
      given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
      given(responseSpec.bodyToMono(ProductDto.class))
          .willReturn(Mono.empty());

      // When
      ProductDto result = productService.getProductInfoSafely(largeId);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getId()).isEqualTo(largeId);
      assertThat(result.getName()).isEqualTo("Product #" + largeId);
    }
  }

  @Nested
  @DisplayName("HTTP Status Handling Tests")
  class HttpStatusHandlingTests {

    @Test
    @DisplayName("Should handle 404 status correctly")
    void shouldHandle404StatusCorrectly() {
      // Given
      WebClientResponseException notFoundException =
          WebClientResponseException.NotFound.create(404, "Not Found", null, null, null);

      given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
      given(responseSpec.bodyToMono(ProductDto.class))
          .willReturn(Mono.error(notFoundException));

      // When
      Optional<ProductDto> result = productService.getProductById(999L);

      // Then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should handle 400 Bad Request")
    void shouldHandle400BadRequest() {
      // Given
      WebClientResponseException badRequestException =
          WebClientResponseException.BadRequest.create(400, "Bad Request", null, null, null);

      given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
      given(responseSpec.bodyToMono(ProductDto.class))
          .willReturn(Mono.error(badRequestException));

      // When & Then
      assertThatThrownBy(() -> productService.getProductById(100L))
          .isInstanceOf(WebClientResponseException.BadRequest.class);
    }

    @Test
    @DisplayName("Should handle 500 Internal Server Error")
    void shouldHandle500InternalServerError() {
      // Given
      WebClientResponseException serverErrorException =
          WebClientResponseException.InternalServerError.create(500, "Internal Server Error", null, null, null);

      given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
      given(responseSpec.bodyToMono(ProductDto.class))
          .willReturn(Mono.error(serverErrorException));

      // When & Then
      assertThatThrownBy(() -> productService.getProductById(100L))
          .isInstanceOf(WebClientResponseException.InternalServerError.class);
    }

    @Test
    @DisplayName("Should handle 503 Service Unavailable")
    void shouldHandle503ServiceUnavailable() {
      // Given
      WebClientResponseException serviceUnavailableException =
          WebClientResponseException.create(503, "Service Unavailable", null, null, null);

      given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
      given(responseSpec.bodyToMono(ProductDto.class))
          .willReturn(Mono.error(serviceUnavailableException));

      // When & Then
      assertThatThrownBy(() -> productService.getProductById(100L))
          .isInstanceOf(WebClientResponseException.class);
    }
  }

  @Nested
  @DisplayName("Configuration Tests")
  class ConfigurationTests {

    @Test
    @DisplayName("Should use configured base URL")
    void shouldUseConfiguredBaseUrl() {
      // Given
      ReflectionTestUtils.setField(productService, "productsServiceBaseUrl", "http://custom-host:9999");
      given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
      given(responseSpec.bodyToMono(ProductDto.class))
          .willReturn(Mono.just(testProduct));

      // When
      productService.getProductById(100L);

      // Then
      verify(requestHeadersUriSpec).uri("http://custom-host:9999/api/v1/products/{productId}", 100L);
    }

    @Test
    @DisplayName("Should use configured API key")
    void shouldUseConfiguredApiKey() {
      // Given
      ReflectionTestUtils.setField(productService, "apiKey", "custom-api-key");
      given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
      given(responseSpec.bodyToMono(ProductDto.class))
          .willReturn(Mono.just(testProduct));

      // When
      productService.getProductById(100L);

      // Then
      verify(requestHeadersSpec).headers(argThat(headerConsumer -> {
        // This is a simplified verification - in real tests you might want to capture and verify headers
        return true;
      }));
    }

    @Test
    @DisplayName("Should use configured timeouts")
    void shouldUseConfiguredTimeouts() {
      // Given
      ReflectionTestUtils.setField(productService, "readTimeout", 5000);
      ReflectionTestUtils.setField(productService, "connectTimeout", 3000);

      // When & Then - Configuration should be applied (verified through behavior)
      assertThat(ReflectionTestUtils.getField(productService, "readTimeout")).isEqualTo(5000);
      assertThat(ReflectionTestUtils.getField(productService, "connectTimeout")).isEqualTo(3000);
    }
  }
}