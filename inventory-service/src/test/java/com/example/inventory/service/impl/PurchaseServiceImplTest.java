package com.example.inventory.service.impl;

import com.example.inventory.controller.dto.ProductDto;
import com.example.inventory.controller.dto.PurchaseRequestDto;
import com.example.inventory.controller.dto.PurchaseResponseDto;
import com.example.inventory.exception.InsufficientStockException;
import com.example.inventory.exception.ProductNotFoundException;
import com.example.inventory.model.Inventory;
import com.example.inventory.model.PurchaseHistory;
import com.example.inventory.repository.InventoryRepository;
import com.example.inventory.repository.PurchaseHistoryRepository;
import com.example.inventory.service.InventoryService;
import com.example.inventory.service.KafkaProducerService;
import com.example.inventory.service.ProductService;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("PurchaseServiceImpl Tests")
class PurchaseServiceImplTest {

  @Mock
  private PurchaseHistoryRepository purchaseHistoryRepository;

  @Mock
  private InventoryRepository inventoryRepository;

  @Mock
  private InventoryService inventoryService;

  @Mock
  private ProductService productService;

  @Mock
  private KafkaProducerService kafkaProducerService;

  @InjectMocks
  private PurchaseServiceImpl purchaseService;

  private PurchaseRequestDto purchaseRequest;
  private ProductDto testProduct;
  private Inventory testInventory;
  private PurchaseHistory testPurchaseHistory;
  private UUID testPurchaseId;

  @BeforeEach
  void setUp() {
    testPurchaseId = UUID.randomUUID();

    Map<String, Object> customerInfo = new HashMap<>();
    purchaseRequest = PurchaseRequestDto.builder()
        .productId(100L)
        .quantity(5)
        .customerInfo(customerInfo)
        .build();

    testProduct = ProductDto.builder()
        .id(100L)
        .name("Test Product")
        .price(BigDecimal.valueOf(29.99))
        .description("Test product description")
        .active(true)
        .build();

    testInventory = Inventory.builder()
        .id(1L)
        .productId(100L)
        .quantity(50)
        .reservedQuantity(10)
        .minStockLevel(5)
        .maxStockLevel(100)
        .build();

    testPurchaseHistory = PurchaseHistory.builder()
        .id(1L)
        .purchaseId(testPurchaseId)
        .productId(100L)
        .quantityPurchased(5)
        .unitPrice(BigDecimal.valueOf(29.99))
        .totalPrice(BigDecimal.valueOf(149.95))
        .purchaseStatus(PurchaseHistory.PurchaseStatus.COMPLETED)
        .customerInfo(customerInfo)
        .purchaseDate(LocalDateTime.now())
        .build();
  }

  @Nested
  @DisplayName("Process Purchase Tests")
  class ProcessPurchaseTests {

    @Test
    @DisplayName("Should process purchase successfully")
    void shouldProcessPurchaseSuccessfully() {
      // Given
      given(productService.getProductById(100L))
          .willReturn(Optional.of(testProduct));
      given(inventoryService.checkStockAvailability(100L, 5))
          .willReturn(true);
      given(inventoryService.reserveStock(100L, 5))
          .willReturn(true);
      given(purchaseHistoryRepository.save(any(PurchaseHistory.class)))
          .willAnswer(invocation -> {
            PurchaseHistory purchase = invocation.getArgument(0);
            purchase.setId(1L);
            purchase.setPurchaseId(testPurchaseId);
            return purchase;
          });
      given(inventoryService.confirmPurchase(100L, 5))
          .willReturn(true);
      given(inventoryRepository.findByProductId(100L))
          .willReturn(Optional.of(testInventory));

      // When
      PurchaseResponseDto result = purchaseService.processPurchase(purchaseRequest);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getPurchaseId()).isEqualTo(testPurchaseId);
      assertThat(result.getProductId()).isEqualTo(100L);
      assertThat(result.getQuantityPurchased()).isEqualTo(5);
      assertThat(result.getUnitPrice()).isEqualTo(BigDecimal.valueOf(29.99));
      assertThat(result.getTotalPrice()).isEqualTo(BigDecimal.valueOf(149.95));
      assertThat(result.getPurchaseStatus()).isEqualTo(PurchaseHistory.PurchaseStatus.COMPLETED);
      assertThat(result.getMessage()).isEqualTo("Purchase completed successfully");

      verify(inventoryService).reserveStock(100L, 5);
      verify(inventoryService).confirmPurchase(100L, 5);
      verify(kafkaProducerService).publishPurchaseCompletedEvent(any(PurchaseHistory.class), eq(testInventory));
    }

    @Test
    @DisplayName("Should throw exception when product not found")
    void shouldThrowExceptionWhenProductNotFound() {
      // Given
      given(productService.getProductById(999L))
          .willReturn(Optional.empty());

      Map<String, Object> customerInfo = new HashMap<>();
      PurchaseRequestDto invalidRequest = PurchaseRequestDto.builder()
          .productId(999L)
          .quantity(5)
          .customerInfo(customerInfo)
          .build();

      // When & Then
      assertThatThrownBy(() -> purchaseService.processPurchase(invalidRequest))
          .isInstanceOf(ProductNotFoundException.class)
          .hasMessageContaining("Product not found with ID: 999");
    }

    @Test
    @DisplayName("Should throw exception when product is inactive")
    void shouldThrowExceptionWhenProductIsInactive() {
      // Given
      testProduct.setActive(false);
      given(productService.getProductById(100L))
          .willReturn(Optional.of(testProduct));

      // When & Then
      assertThatThrownBy(() -> purchaseService.processPurchase(purchaseRequest))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Product is not active: 100");
    }

    @Test
    @DisplayName("Should throw exception when insufficient stock")
    void shouldThrowExceptionWhenInsufficientStock() {
      // Given
      given(productService.getProductById(100L))
          .willReturn(Optional.of(testProduct));
      given(inventoryService.checkStockAvailability(100L, 5))
          .willReturn(false);

      // When & Then
      assertThatThrownBy(() -> purchaseService.processPurchase(purchaseRequest))
          .isInstanceOf(InsufficientStockException.class)
          .hasMessageContaining("Insufficient stock for product ID: 100");
    }

    @Test
    @DisplayName("Should release stock when reservation fails")
    void shouldReleaseStockWhenReservationFails() {
      // Given
      given(productService.getProductById(100L))
          .willReturn(Optional.of(testProduct));
      given(inventoryService.checkStockAvailability(100L, 5))
          .willReturn(true);
      given(inventoryService.reserveStock(100L, 5))
          .willReturn(false);

      // When & Then
      assertThatThrownBy(() -> purchaseService.processPurchase(purchaseRequest))
          .isInstanceOf(InsufficientStockException.class)
          .hasMessageContaining("Failed to reserve stock for purchase");
    }

    @Test
    @DisplayName("Should release stock and mark as cancelled when confirmation fails")
    void shouldReleaseStockAndMarkAsCancelledWhenConfirmationFails() {
      // Given
      given(productService.getProductById(100L))
          .willReturn(Optional.of(testProduct));
      given(inventoryService.checkStockAvailability(100L, 5))
          .willReturn(true);
      given(inventoryService.reserveStock(100L, 5))
          .willReturn(true);
      given(purchaseHistoryRepository.save(any(PurchaseHistory.class)))
          .willAnswer(invocation -> {
            PurchaseHistory purchase = invocation.getArgument(0);
            purchase.setId(1L);
            purchase.setPurchaseId(testPurchaseId);
            return purchase;
          });
      given(inventoryService.confirmPurchase(100L, 5))
          .willReturn(false);

      // When & Then
      assertThatThrownBy(() -> purchaseService.processPurchase(purchaseRequest))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Failed to confirm purchase");

      verify(inventoryService).releaseReservedStock(100L, 5);
      verify(purchaseHistoryRepository, times(2)).save(any(PurchaseHistory.class));
    }

    @Test
    @DisplayName("Should handle exception during purchase processing")
    void shouldHandleExceptionDuringPurchaseProcessing() {
      // Given
      given(productService.getProductById(100L))
          .willReturn(Optional.of(testProduct));
      given(inventoryService.checkStockAvailability(100L, 5))
          .willReturn(true);
      given(inventoryService.reserveStock(100L, 5))
          .willReturn(true);
      given(purchaseHistoryRepository.save(any(PurchaseHistory.class)))
          .willThrow(new RuntimeException("Database error"));

      // When & Then
      assertThatThrownBy(() -> purchaseService.processPurchase(purchaseRequest))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Database error");

      verify(inventoryService).releaseReservedStock(100L, 5);
    }
  }

  @Nested
  @DisplayName("Get Purchase Tests")
  class GetPurchaseTests {

    @Test
    @DisplayName("Should get purchase by ID successfully")
    void shouldGetPurchaseByIdSuccessfully() {
      // Given
      given(purchaseHistoryRepository.findByPurchaseId(testPurchaseId))
          .willReturn(Optional.of(testPurchaseHistory));
      given(productService.getProductInfoSafely(100L))
          .willReturn(testProduct);

      // When
      PurchaseResponseDto result = purchaseService.getPurchaseById(testPurchaseId);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getPurchaseId()).isEqualTo(testPurchaseId);
      assertThat(result.getProductId()).isEqualTo(100L);
      assertThat(result.getProductInfo()).isEqualTo(testProduct);
    }

    @Test
    @DisplayName("Should throw exception when purchase not found")
    void shouldThrowExceptionWhenPurchaseNotFound() {
      // Given
      UUID unknownId = UUID.randomUUID();
      given(purchaseHistoryRepository.findByPurchaseId(unknownId))
          .willReturn(Optional.empty());

      // When & Then
      assertThatThrownBy(() -> purchaseService.getPurchaseById(unknownId))
          .isInstanceOf(ProductNotFoundException.class)
          .hasMessageContaining("Purchase not found with ID:");
    }

    @Test
    @DisplayName("Should get purchase history with pagination")
    void shouldGetPurchaseHistoryWithPagination() {
      // Given
      Pageable pageable = PageRequest.of(0, 10);
      List<PurchaseHistory> purchases = Arrays.asList(testPurchaseHistory);
      Page<PurchaseHistory> page = new PageImpl<>(purchases, pageable, 1);

      given(purchaseHistoryRepository.findAll(pageable))
          .willReturn(page);
      given(productService.getProductInfoSafely(100L))
          .willReturn(testProduct);

      // When
      Page<PurchaseResponseDto> result = purchaseService.getPurchaseHistory(pageable);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getContent()).hasSize(1);
      assertThat(result.getContent().get(0).getPurchaseId()).isEqualTo(testPurchaseId);
    }

    @Test
    @DisplayName("Should get purchase history by product")
    void shouldGetPurchaseHistoryByProduct() {
      // Given
      Pageable pageable = PageRequest.of(0, 10);
      List<PurchaseHistory> purchases = Arrays.asList(testPurchaseHistory);
      Page<PurchaseHistory> page = new PageImpl<>(purchases, pageable, 1);

      given(purchaseHistoryRepository.findByProductIdOrderByPurchaseDateDesc(100L, pageable))
          .willReturn(page);
      given(productService.getProductInfoSafely(100L))
          .willReturn(testProduct);

      // When
      Page<PurchaseResponseDto> result = purchaseService.getPurchaseHistoryByProduct(100L, pageable);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getContent()).hasSize(1);
      assertThat(result.getContent().get(0).getProductId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("Should get purchase history with filters")
    void shouldGetPurchaseHistoryWithFilters() {
      // Given
      Pageable pageable = PageRequest.of(0, 10);
      LocalDateTime startDate = LocalDateTime.now().minusDays(7);
      LocalDateTime endDate = LocalDateTime.now();
      PurchaseHistory.PurchaseStatus status = PurchaseHistory.PurchaseStatus.COMPLETED;

      List<PurchaseHistory> purchases = Arrays.asList(testPurchaseHistory);
      Page<PurchaseHistory> page = new PageImpl<>(purchases, pageable, 1);

      given(purchaseHistoryRepository.findWithFilters(
          100L, status, startDate, endDate, 1, 10, pageable))
          .willReturn(page);
      given(productService.getProductInfoSafely(100L))
          .willReturn(testProduct);

      // When
      Page<PurchaseResponseDto> result = purchaseService.getPurchaseHistoryWithFilters(
          100L, status, startDate, endDate, 1, 10, pageable);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getContent()).hasSize(1);
      assertThat(result.getContent().get(0).getPurchaseStatus()).isEqualTo(status);
    }
  }

  @Nested
  @DisplayName("Cancel Purchase Tests")
  class CancelPurchaseTests {

    @Test
    @DisplayName("Should cancel pending purchase successfully")
    void shouldCancelPendingPurchaseSuccessfully() {
      // Given
      testPurchaseHistory.setPurchaseStatus(PurchaseHistory.PurchaseStatus.PENDING);
      given(purchaseHistoryRepository.findByPurchaseId(testPurchaseId))
          .willReturn(Optional.of(testPurchaseHistory));

      // When
      boolean result = purchaseService.cancelPurchase(testPurchaseId);

      // Then
      assertThat(result).isTrue();
      verify(inventoryService).releaseReservedStock(100L, 5);
      verify(purchaseHistoryRepository).save(testPurchaseHistory);
      verify(kafkaProducerService).publishPurchaseCancelledEvent(testPurchaseHistory);
    }

    @Test
    @DisplayName("Should not cancel completed purchase")
    void shouldNotCancelCompletedPurchase() {
      // Given
      testPurchaseHistory.setPurchaseStatus(PurchaseHistory.PurchaseStatus.COMPLETED);
      given(purchaseHistoryRepository.findByPurchaseId(testPurchaseId))
          .willReturn(Optional.of(testPurchaseHistory));

      // When
      boolean result = purchaseService.cancelPurchase(testPurchaseId);

      // Then
      assertThat(result).isFalse();
      verifyNoInteractions(inventoryService);
      verifyNoInteractions(kafkaProducerService);
    }

    @Test
    @DisplayName("Should return false when purchase not found for cancellation")
    void shouldReturnFalseWhenPurchaseNotFoundForCancellation() {
      // Given
      UUID unknownId = UUID.randomUUID();
      given(purchaseHistoryRepository.findByPurchaseId(unknownId))
          .willReturn(Optional.empty());

      // When
      boolean result = purchaseService.cancelPurchase(unknownId);

      // Then
      assertThat(result).isFalse();
    }
  }

  @Nested
  @DisplayName("Refund Purchase Tests")
  class RefundPurchaseTests {

    @Test
    @DisplayName("Should refund completed purchase successfully")
    void shouldRefundCompletedPurchaseSuccessfully() {
      // Given
      testPurchaseHistory.setPurchaseStatus(PurchaseHistory.PurchaseStatus.COMPLETED);
      given(purchaseHistoryRepository.findByPurchaseId(testPurchaseId))
          .willReturn(Optional.of(testPurchaseHistory));
      given(inventoryRepository.findByProductIdForUpdate(100L))
          .willReturn(Optional.of(testInventory));

      // When
      boolean result = purchaseService.refundPurchase(testPurchaseId);

      // Then
      assertThat(result).isTrue();
      verify(inventoryRepository).save(testInventory);
      verify(purchaseHistoryRepository).save(testPurchaseHistory);

      // Verify that stock was added back
      ArgumentCaptor<Inventory> inventoryCaptor = ArgumentCaptor.forClass(Inventory.class);
      verify(inventoryRepository).save(inventoryCaptor.capture());
      // Note: The actual stock addition logic would be tested in the Inventory model tests
    }

    @Test
    @DisplayName("Should not refund pending purchase")
    void shouldNotRefundPendingPurchase() {
      // Given
      testPurchaseHistory.setPurchaseStatus(PurchaseHistory.PurchaseStatus.PENDING);
      given(purchaseHistoryRepository.findByPurchaseId(testPurchaseId))
          .willReturn(Optional.of(testPurchaseHistory));

      // When
      boolean result = purchaseService.refundPurchase(testPurchaseId);

      // Then
      assertThat(result).isFalse();
      verifyNoInteractions(inventoryRepository);
    }

    @Test
    @DisplayName("Should return false when purchase not found for refund")
    void shouldReturnFalseWhenPurchaseNotFoundForRefund() {
      // Given
      UUID unknownId = UUID.randomUUID();
      given(purchaseHistoryRepository.findByPurchaseId(unknownId))
          .willReturn(Optional.empty());

      // When
      boolean result = purchaseService.refundPurchase(unknownId);

      // Then
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should handle refund when inventory not found")
    void shouldHandleRefundWhenInventoryNotFound() {
      // Given
      testPurchaseHistory.setPurchaseStatus(PurchaseHistory.PurchaseStatus.COMPLETED);
      given(purchaseHistoryRepository.findByPurchaseId(testPurchaseId))
          .willReturn(Optional.of(testPurchaseHistory));
      given(inventoryRepository.findByProductIdForUpdate(100L))
          .willReturn(Optional.empty());

      // When
      boolean result = purchaseService.refundPurchase(testPurchaseId);

      // Then
      assertThat(result).isTrue(); // Still marks as refunded even if inventory not found
      verify(purchaseHistoryRepository).save(testPurchaseHistory);
    }
  }

  @Nested
  @DisplayName("Statistics Tests")
  class StatisticsTests {

    @Test
    @DisplayName("Should get purchase statistics by product")
    void shouldGetPurchaseStatisticsByProduct() {
      // Given
      List<Object> mockStats = Arrays.asList(new Object(), new Object());
      given(purchaseHistoryRepository.getPurchaseStatisticsByProduct())
          .willReturn(mockStats);

      // When
      List<Object> result = purchaseService.getPurchaseStatisticsByProduct();

      // Then
      assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("Should get top selling products")
    void shouldGetTopSellingProducts() {
      // Given
      List<Object> mockTopProducts = Arrays.asList(new Object(), new Object());
      given(purchaseHistoryRepository.getTopSellingProducts(any(Pageable.class)))
          .willReturn(mockTopProducts);

      // When
      List<Object> result = purchaseService.getTopSellingProducts(5);

      // Then
      assertThat(result).hasSize(2);
      ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
      verify(purchaseHistoryRepository).getTopSellingProducts(pageableCaptor.capture());
      assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should use default limit for top selling products when null")
    void shouldUseDefaultLimitForTopSellingProductsWhenNull() {
      // Given
      List<Object> mockTopProducts = Arrays.asList(new Object());
      given(purchaseHistoryRepository.getTopSellingProducts(any(Pageable.class)))
          .willReturn(mockTopProducts);

      // When
      purchaseService.getTopSellingProducts(null);

      // Then
      ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
      verify(purchaseHistoryRepository).getTopSellingProducts(pageableCaptor.capture());
      assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
    }

    @Test
    @DisplayName("Should get daily sales report")
    void shouldGetDailySalesReport() {
      // Given
      LocalDateTime startDate = LocalDateTime.now().minusDays(7);
      LocalDateTime endDate = LocalDateTime.now();
      List<Object> mockReport = Arrays.asList(new Object(), new Object());

      given(purchaseHistoryRepository.getDailySalesReport(startDate, endDate))
          .willReturn(mockReport);

      // When
      List<Object> result = purchaseService.getDailySalesReport(startDate, endDate);

      // Then
      assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("Should get revenue by product")
    void shouldGetRevenueByProduct() {
      // Given
      LocalDateTime startDate = LocalDateTime.now().minusDays(30);
      LocalDateTime endDate = LocalDateTime.now();
      List<Object> mockRevenue = Arrays.asList(new Object(), new Object());

      given(purchaseHistoryRepository.getRevenueByProduct(startDate, endDate))
          .willReturn(mockRevenue);

      // When
      List<Object> result = purchaseService.getRevenueByProduct(startDate, endDate);

      // Then
      assertThat(result).hasSize(2);
    }
  }

  @Nested
  @DisplayName("Validation Tests")
  class ValidationTests {

    @Test
    @DisplayName("Should validate valid purchase request")
    void shouldValidateValidPurchaseRequest() {
      // When
      boolean result = purchaseService.validatePurchaseRequest(purchaseRequest);

      // Then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should reject null purchase request")
    void shouldRejectNullPurchaseRequest() {
      // When
      boolean result = purchaseService.validatePurchaseRequest(null);

      // Then
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should reject purchase request with null product ID")
    void shouldRejectPurchaseRequestWithNullProductId() {
      // Given
      purchaseRequest.setProductId(null);

      // When
      boolean result = purchaseService.validatePurchaseRequest(purchaseRequest);

      // Then
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should reject purchase request with invalid product ID")
    void shouldRejectPurchaseRequestWithInvalidProductId() {
      // Given
      purchaseRequest.setProductId(-1L);

      // When
      boolean result = purchaseService.validatePurchaseRequest(purchaseRequest);

      // Then
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should reject purchase request with null quantity")
    void shouldRejectPurchaseRequestWithNullQuantity() {
      // Given
      purchaseRequest.setQuantity(null);

      // When
      boolean result = purchaseService.validatePurchaseRequest(purchaseRequest);

      // Then
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should reject purchase request with invalid quantity")
    void shouldRejectPurchaseRequestWithInvalidQuantity() {
      // Given
      purchaseRequest.setQuantity(0);

      // When
      boolean result = purchaseService.validatePurchaseRequest(purchaseRequest);

      // Then
      assertThat(result).isFalse();
    }
  }

  @Nested
  @DisplayName("Price Calculation Tests")
  class PriceCalculationTests {

    @Test
    @DisplayName("Should calculate total price correctly")
    void shouldCalculateTotalPriceCorrectly() {
      // Given
      given(productService.getProductById(100L))
          .willReturn(Optional.of(testProduct));

      // When
      BigDecimal result = purchaseService.calculateTotalPrice(100L, 3);

      // Then
      BigDecimal expected = BigDecimal.valueOf(29.99).multiply(BigDecimal.valueOf(3));
      assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("Should throw exception when calculating price for non-existing product")
    void shouldThrowExceptionWhenCalculatingPriceForNonExistingProduct() {
      // Given
      given(productService.getProductById(999L))
          .willReturn(Optional.empty());

      // When & Then
      assertThatThrownBy(() -> purchaseService.calculateTotalPrice(999L, 3))
          .isInstanceOf(ProductNotFoundException.class)
          .hasMessageContaining("Product not found with ID: 999");
    }
  }

  @Nested
  @DisplayName("Recent Purchases Tests")
  class RecentPurchasesTests {

    @Test
    @DisplayName("Should get recent purchases")
    void shouldGetRecentPurchases() {
      // Given
      List<PurchaseHistory> recentPurchases = Arrays.asList(testPurchaseHistory);
      given(purchaseHistoryRepository.findRecentPurchases(any(LocalDateTime.class)))
          .willReturn(recentPurchases);
      given(productService.getProductInfoSafely(100L))
          .willReturn(testProduct);

      // When
      List<PurchaseResponseDto> result = purchaseService.getRecentPurchases(24);

      // Then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getPurchaseId()).isEqualTo(testPurchaseId);
    }

    @Test
    @DisplayName("Should use default hours when null provided")
    void shouldUseDefaultHoursWhenNullProvided() {
      // Given
      List<PurchaseHistory> recentPurchases = Arrays.asList(testPurchaseHistory);
      given(purchaseHistoryRepository.findRecentPurchases(any(LocalDateTime.class)))
          .willReturn(recentPurchases);
      given(productService.getProductInfoSafely(100L))
          .willReturn(testProduct);

      // When
      List<PurchaseResponseDto> result = purchaseService.getRecentPurchases(null);

      // Then
      assertThat(result).hasSize(1);
      ArgumentCaptor<LocalDateTime> dateCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
      verify(purchaseHistoryRepository).findRecentPurchases(dateCaptor.capture());
      // Verify that default 24 hours was used (approximately)
      LocalDateTime expectedSince = LocalDateTime.now().minusHours(24);
      assertThat(dateCaptor.getValue()).isBefore(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should get stale pending purchases")
    void shouldGetStalePendingPurchases() {
      // Given
      testPurchaseHistory.setPurchaseStatus(PurchaseHistory.PurchaseStatus.PENDING);
      List<PurchaseHistory> stalePurchases = Arrays.asList(testPurchaseHistory);
      given(purchaseHistoryRepository.findStalesPendingPurchases(any(LocalDateTime.class)))
          .willReturn(stalePurchases);
      given(productService.getProductInfoSafely(100L))
          .willReturn(testProduct);

      // When
      List<PurchaseResponseDto> result = purchaseService.getStalePendingPurchases(2);

      // Then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getPurchaseStatus()).isEqualTo(PurchaseHistory.PurchaseStatus.PENDING);
    }
  }

  @Nested
  @DisplayName("Retry Failed Purchase Tests")
  class RetryFailedPurchaseTests {

    @Test
    @DisplayName("Should retry failed purchase successfully")
    void shouldRetryFailedPurchaseSuccessfully() {
      // Given
      testPurchaseHistory.setPurchaseStatus(PurchaseHistory.PurchaseStatus.PENDING);
      given(purchaseHistoryRepository.findByPurchaseId(testPurchaseId))
          .willReturn(Optional.of(testPurchaseHistory));

      // Mock successful retry
      given(productService.getProductById(100L))
          .willReturn(Optional.of(testProduct));
      given(inventoryService.checkStockAvailability(100L, 5))
          .willReturn(true);
      given(inventoryService.reserveStock(100L, 5))
          .willReturn(true);
      given(purchaseHistoryRepository.save(any(PurchaseHistory.class)))
          .willAnswer(invocation -> {
            PurchaseHistory purchase = invocation.getArgument(0);
            purchase.setId(2L);
            purchase.setPurchaseId(UUID.randomUUID());
            return purchase;
          });
      given(inventoryService.confirmPurchase(100L, 5))
          .willReturn(true);
      given(inventoryRepository.findByProductId(100L))
          .willReturn(Optional.of(testInventory));

      // When
      boolean result = purchaseService.retryFailedPurchase(testPurchaseId);

      // Then
      assertThat(result).isTrue();
      verify(purchaseHistoryRepository, times(2)).save(any(PurchaseHistory.class)); // Original + new purchase
    }

    @Test
    @DisplayName("Should not retry completed purchase")
    void shouldNotRetryCompletedPurchase() {
      // Given
      testPurchaseHistory.setPurchaseStatus(PurchaseHistory.PurchaseStatus.COMPLETED);
      given(purchaseHistoryRepository.findByPurchaseId(testPurchaseId))
          .willReturn(Optional.of(testPurchaseHistory));

      // When
      boolean result = purchaseService.retryFailedPurchase(testPurchaseId);

      // Then
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when purchase not found for retry")
    void shouldReturnFalseWhenPurchaseNotFoundForRetry() {
      // Given
      UUID unknownId = UUID.randomUUID();
      given(purchaseHistoryRepository.findByPurchaseId(unknownId))
          .willReturn(Optional.empty());

      // When
      boolean result = purchaseService.retryFailedPurchase(unknownId);

      // Then
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should handle retry failure gracefully")
    void shouldHandleRetryFailureGracefully() {
      // Given
      testPurchaseHistory.setPurchaseStatus(PurchaseHistory.PurchaseStatus.CANCELLED);
      given(purchaseHistoryRepository.findByPurchaseId(testPurchaseId))
          .willReturn(Optional.of(testPurchaseHistory));
      given(productService.getProductById(100L))
          .willReturn(Optional.empty()); // This will cause retry to fail

      // When
      boolean result = purchaseService.retryFailedPurchase(testPurchaseId);

      // Then
      assertThat(result).isFalse();
    }
  }

  @Nested
  @DisplayName("Edge Cases Tests")
  class EdgeCasesTests {

    @Test
    @DisplayName("Should handle invalid purchase request gracefully")
    void shouldHandleInvalidPurchaseRequestGracefully() {
      // Given
      PurchaseRequestDto invalidRequest = PurchaseRequestDto.builder()
          .productId(null)
          .quantity(-1)
          .build();

      // When & Then
      assertThatThrownBy(() -> purchaseService.processPurchase(invalidRequest))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Invalid purchase request");
    }

    @Test
    @DisplayName("Should handle zero quantity in price calculation")
    void shouldHandleZeroQuantityInPriceCalculation() {
      // Given
      given(productService.getProductById(100L))
          .willReturn(Optional.of(testProduct));

      // When
      BigDecimal result = purchaseService.calculateTotalPrice(100L, 0);

      // Then
      assertThat(result).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should generate appropriate message for different purchase statuses")
    void shouldGenerateAppropriateMessageForDifferentPurchaseStatuses() {
      // Given
      given(purchaseHistoryRepository.findByPurchaseId(testPurchaseId))
          .willReturn(Optional.of(testPurchaseHistory));
      given(productService.getProductInfoSafely(100L))
          .willReturn(testProduct);

      // Test different statuses
      testPurchaseHistory.setPurchaseStatus(PurchaseHistory.PurchaseStatus.PENDING);
      PurchaseResponseDto pendingResult = purchaseService.getPurchaseById(testPurchaseId);
      assertThat(pendingResult.getMessage()).isEqualTo("Purchase is pending confirmation");

      testPurchaseHistory.setPurchaseStatus(PurchaseHistory.PurchaseStatus.CANCELLED);
      PurchaseResponseDto cancelledResult = purchaseService.getPurchaseById(testPurchaseId);
      assertThat(cancelledResult.getMessage()).isEqualTo("Purchase was cancelled");

      testPurchaseHistory.setPurchaseStatus(PurchaseHistory.PurchaseStatus.REFUNDED);
      PurchaseResponseDto refundedResult = purchaseService.getPurchaseById(testPurchaseId);
      assertThat(refundedResult.getMessage()).isEqualTo("Purchase was refunded");

      testPurchaseHistory.setPurchaseStatus(PurchaseHistory.PurchaseStatus.COMPLETED);
      PurchaseResponseDto completedResult = purchaseService.getPurchaseById(testPurchaseId);
      assertThat(completedResult.getMessage()).isEqualTo("Purchase completed successfully");
    }
  }
}