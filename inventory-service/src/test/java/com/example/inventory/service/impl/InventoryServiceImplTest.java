package com.example.inventory.service.impl;

import com.example.inventory.controller.dto.InventoryDto;
import com.example.inventory.controller.dto.ProductDto;
import com.example.inventory.controller.dto.UpdateInventoryRequestDto;
import com.example.inventory.exception.InsufficientStockException;
import com.example.inventory.exception.ProductNotFoundException;
import com.example.inventory.model.Inventory;
import com.example.inventory.model.InventoryMovement;
import com.example.inventory.repository.InventoryMovementRepository;
import com.example.inventory.repository.InventoryRepository;
import com.example.inventory.service.KafkaProducerService;
import com.example.inventory.service.ProductService;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryServiceImpl Tests")
class InventoryServiceImplTest {

  @Mock
  private InventoryRepository inventoryRepository;

  @Mock
  private InventoryMovementRepository inventoryMovementRepository;

  @Mock
  private ProductService productService;

  @Mock
  private KafkaProducerService kafkaProducerService;

  @InjectMocks
  private InventoryServiceImpl inventoryService;

  private Inventory testInventory;
  private ProductDto testProduct;
  private UpdateInventoryRequestDto updateRequest;

  @BeforeEach
  void setUp() {
    testInventory = Inventory.builder()
        .id(1L)
        .productId(100L)
        .quantity(50)
        .reservedQuantity(10)
        .minStockLevel(5)
        .maxStockLevel(100)
        .build();

    testProduct = ProductDto.builder()
        .id(100L)
        .name("Test Product")
        .price(BigDecimal.valueOf(29.99))
        .description("Test product description")
        .active(true)
        .build();

    updateRequest = UpdateInventoryRequestDto.builder()
        .quantity(75)
        .minStockLevel(10)
        .maxStockLevel(150)
        .reason("Test update")
        .referenceId("REF-123")
        .build();
  }

  @Nested
  @DisplayName("Get Inventory Tests")
  class GetInventoryTests {

    @Test
    @DisplayName("Should return inventory when product exists")
    void shouldReturnInventoryWhenProductExists() {
      // Given
      given(inventoryRepository.findByProductId(100L))
          .willReturn(Optional.of(testInventory));
      given(productService.getProductInfoSafely(100L))
          .willReturn(testProduct);

      // When
      InventoryDto result = inventoryService.getInventoryByProductId(100L);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getProductId()).isEqualTo(100L);
      assertThat(result.getQuantity()).isEqualTo(50);
      assertThat(result.getReservedQuantity()).isEqualTo(10);
      assertThat(result.getAvailableQuantity()).isEqualTo(40);
      assertThat(result.getProductInfo()).isEqualTo(testProduct);
    }

    @Test
    @DisplayName("Should throw exception when inventory not found")
    void shouldThrowExceptionWhenInventoryNotFound() {
      // Given
      given(inventoryRepository.findByProductId(999L))
          .willReturn(Optional.empty());

      // When & Then
      assertThatThrownBy(() -> inventoryService.getInventoryByProductId(999L))
          .isInstanceOf(ProductNotFoundException.class)
          .hasMessageContaining("Inventory not found for product ID: 999");
    }
  }

  @Nested
  @DisplayName("Update Inventory Tests")
  class UpdateInventoryTests {

    @Test
    @DisplayName("Should update inventory successfully")
    void shouldUpdateInventorySuccessfully() {
      // Given
      given(inventoryRepository.findByProductIdForUpdate(100L))
          .willReturn(Optional.of(testInventory));
      given(inventoryRepository.save(any(Inventory.class)))
          .willReturn(testInventory);
      given(productService.getProductInfoSafely(100L))
          .willReturn(testProduct);

      // When
      InventoryDto result = inventoryService.updateInventory(100L, updateRequest);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getQuantity()).isEqualTo(75);

      // Verify inventory movement was created
      ArgumentCaptor<InventoryMovement> movementCaptor = ArgumentCaptor.forClass(InventoryMovement.class);
      verify(inventoryMovementRepository).save(movementCaptor.capture());

      InventoryMovement savedMovement = movementCaptor.getValue();
      assertThat(savedMovement.getProductId()).isEqualTo(100L);
      assertThat(savedMovement.getReason()).isEqualTo("Test update");

      // Verify Kafka events were published
      verify(kafkaProducerService).publishInventoryUpdatedEvent(testInventory);
    }

    @Test
    @DisplayName("Should throw exception when quantity below reserved amount")
    void shouldThrowExceptionWhenQuantityBelowReservedAmount() {
      // Given
      testInventory.setReservedQuantity(20);
      updateRequest.setQuantity(15); // Less than reserved quantity

      given(inventoryRepository.findByProductIdForUpdate(100L))
          .willReturn(Optional.of(testInventory));

      // When & Then
      assertThatThrownBy(() -> inventoryService.updateInventory(100L, updateRequest))
          .isInstanceOf(InsufficientStockException.class)
          .hasMessageContaining("Cannot set quantity below reserved amount");
    }
  }

  @Nested
  @DisplayName("Stock Operations Tests")
  class StockOperationsTests {

    @Test
    @DisplayName("Should check stock availability successfully")
    void shouldCheckStockAvailabilitySuccessfully() {
      // Given
      given(inventoryRepository.findByProductId(100L))
          .willReturn(Optional.of(testInventory));

      // When
      boolean hasStock = inventoryService.checkStockAvailability(100L, 30);

      // Then
      assertThat(hasStock).isTrue(); // Available: 50 - 10 = 40, requested: 30
    }

    @Test
    @DisplayName("Should return false when insufficient stock")
    void shouldReturnFalseWhenInsufficientStock() {
      // Given
      given(inventoryRepository.findByProductId(100L))
          .willReturn(Optional.of(testInventory));

      // When
      boolean hasStock = inventoryService.checkStockAvailability(100L, 50);

      // Then
      assertThat(hasStock).isFalse(); // Available: 50 - 10 = 40, requested: 50
    }

    @Test
    @DisplayName("Should return false when inventory not found")
    void shouldReturnFalseWhenInventoryNotFound() {
      // Given
      given(inventoryRepository.findByProductId(999L))
          .willReturn(Optional.empty());

      // When
      boolean hasStock = inventoryService.checkStockAvailability(999L, 10);

      // Then
      assertThat(hasStock).isFalse();
    }

    @Test
    @DisplayName("Should reserve stock successfully")
    void shouldReserveStockSuccessfully() {
      // Given
      given(inventoryRepository.reserveStockAtomically(100L, 20))
          .willReturn(1);
      given(inventoryRepository.findByProductId(100L))
          .willReturn(Optional.of(testInventory));

      // When
      boolean result = inventoryService.reserveStock(100L, 20);

      // Then
      assertThat(result).isTrue();
      verify(inventoryMovementRepository).save(any(InventoryMovement.class));
      verify(kafkaProducerService).publishStockReservedEvent(100L, 20, null);
    }

    @Test
    @DisplayName("Should fail to reserve when insufficient stock")
    void shouldFailToReserveWhenInsufficientStock() {
      // Given
      given(inventoryRepository.reserveStockAtomically(100L, 50))
          .willReturn(0);

      // When
      boolean result = inventoryService.reserveStock(100L, 50);

      // Then
      assertThat(result).isFalse();
      verifyNoInteractions(inventoryMovementRepository);
      verifyNoInteractions(kafkaProducerService);
    }

    @Test
    @DisplayName("Should release reserved stock successfully")
    void shouldReleaseReservedStockSuccessfully() {
      // Given
      given(inventoryRepository.releaseReservedStockAtomically(100L, 5))
          .willReturn(1);
      given(inventoryRepository.findByProductId(100L))
          .willReturn(Optional.of(testInventory));

      // When
      boolean result = inventoryService.releaseReservedStock(100L, 5);

      // Then
      assertThat(result).isTrue();
      verify(inventoryMovementRepository).save(any(InventoryMovement.class));
      verify(kafkaProducerService).publishStockReleasedEvent(100L, 5, null);
    }

    @Test
    @DisplayName("Should confirm purchase successfully")
    void shouldConfirmPurchaseSuccessfully() {
      // Given
      given(inventoryRepository.confirmPurchaseAtomically(100L, 10))
          .willReturn(1);
      given(inventoryRepository.findByProductId(100L))
          .willReturn(Optional.of(testInventory));

      // When
      boolean result = inventoryService.confirmPurchase(100L, 10);

      // Then
      assertThat(result).isTrue();
      verify(inventoryMovementRepository).save(any(InventoryMovement.class));
    }
  }

  @Nested
  @DisplayName("Create Inventory Tests")
  class CreateInventoryTests {

    @Test
    @DisplayName("Should create inventory successfully")
    void shouldCreateInventorySuccessfully() {
      // Given
      given(productService.getProductById(200L))
          .willReturn(Optional.of(testProduct));
      given(inventoryRepository.existsByProductId(200L))
          .willReturn(false);
      given(inventoryRepository.save(any(Inventory.class)))
          .willAnswer(invocation -> {
            Inventory inv = invocation.getArgument(0);
            inv.setId(1L);
            return inv;
          });
      given(productService.getProductInfoSafely(200L))
          .willReturn(testProduct);

      // When
      InventoryDto result = inventoryService.createInventory(200L, 100, 10, 500);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getProductId()).isEqualTo(200L);
      assertThat(result.getQuantity()).isEqualTo(100);
      assertThat(result.getMinStockLevel()).isEqualTo(10);
      assertThat(result.getMaxStockLevel()).isEqualTo(500);

      verify(inventoryMovementRepository).save(any(InventoryMovement.class));
      verify(kafkaProducerService).publishInventoryUpdatedEvent(any(Inventory.class));
    }

    @Test
    @DisplayName("Should throw exception when product not found")
    void shouldThrowExceptionWhenProductNotFound() {
      // Given
      given(productService.getProductById(999L))
          .willReturn(Optional.empty());

      // When & Then
      assertThatThrownBy(() -> inventoryService.createInventory(999L, 100, 10, 500))
          .isInstanceOf(ProductNotFoundException.class)
          .hasMessageContaining("Product not found with ID: 999");
    }

    @Test
    @DisplayName("Should throw exception when inventory already exists")
    void shouldThrowExceptionWhenInventoryAlreadyExists() {
      // Given
      given(productService.getProductById(100L))
          .willReturn(Optional.of(testProduct));
      given(inventoryRepository.existsByProductId(100L))
          .willReturn(true);

      // When & Then
      assertThatThrownBy(() -> inventoryService.createInventory(100L, 100, 10, 500))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Inventory already exists for product ID: 100");
    }
  }

  @Nested
  @DisplayName("Filter and Search Tests")
  class FilterAndSearchTests {

    @Test
    @DisplayName("Should get all inventories with pagination")
    void shouldGetAllInventoriesWithPagination() {
      // Given
      Pageable pageable = PageRequest.of(0, 10);
      List<Inventory> inventories = Arrays.asList(testInventory);
      Page<Inventory> page = new PageImpl<>(inventories, pageable, 1);

      given(inventoryRepository.findAllByOrderByUpdatedAtDesc(pageable))
          .willReturn(page);
      given(productService.getProductInfoSafely(100L))
          .willReturn(testProduct);

      // When
      Page<InventoryDto> result = inventoryService.getAllInventories(pageable);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getContent()).hasSize(1);
      assertThat(result.getContent().get(0).getProductId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("Should get inventories with filters")
    void shouldGetInventoriesWithFilters() {
      // Given
      Pageable pageable = PageRequest.of(0, 10);
      List<Inventory> inventories = Arrays.asList(testInventory);
      Page<Inventory> page = new PageImpl<>(inventories, pageable, 1);

      given(inventoryRepository.findByFilters(true, false, 10, 100, pageable))
          .willReturn(page);
      given(productService.getProductInfoSafely(100L))
          .willReturn(testProduct);

      // When
      Page<InventoryDto> result = inventoryService.getInventoriesWithFilters(
          true, false, 10, 100, pageable);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Should get low stock products")
    void shouldGetLowStockProducts() {
      // Given
      testInventory.setQuantity(3); // Below min stock level
      List<Inventory> lowStockInventories = Arrays.asList(testInventory);

      given(inventoryRepository.findLowStockProducts())
          .willReturn(lowStockInventories);
      given(productService.getProductInfoSafely(100L))
          .willReturn(testProduct);

      // When
      List<InventoryDto> result = inventoryService.getLowStockProducts();

      // Then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getProductId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("Should get out of stock products")
    void shouldGetOutOfStockProducts() {
      // Given
      testInventory.setQuantity(0);
      List<Inventory> outOfStockInventories = Arrays.asList(testInventory);

      given(inventoryRepository.findOutOfStockProducts())
          .willReturn(outOfStockInventories);
      given(productService.getProductInfoSafely(100L))
          .willReturn(testProduct);

      // When
      List<InventoryDto> result = inventoryService.getOutOfStockProducts();

      // Then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getQuantity()).isEqualTo(0);
    }
  }

  @Nested
  @DisplayName("Delete Inventory Tests")
  class DeleteInventoryTests {

    @Test
    @DisplayName("Should delete inventory successfully")
    void shouldDeleteInventorySuccessfully() {
      // Given
      testInventory.setReservedQuantity(0); // No reserved stock
      given(inventoryRepository.findByProductId(100L))
          .willReturn(Optional.of(testInventory));

      // When
      inventoryService.deleteInventory(100L);

      // Then
      verify(inventoryRepository).delete(testInventory);
    }

    @Test
    @DisplayName("Should throw exception when deleting inventory with reserved stock")
    void shouldThrowExceptionWhenDeletingInventoryWithReservedStock() {
      // Given
      testInventory.setReservedQuantity(10); // Has reserved stock
      given(inventoryRepository.findByProductId(100L))
          .willReturn(Optional.of(testInventory));

      // When & Then
      assertThatThrownBy(() -> inventoryService.deleteInventory(100L))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Cannot delete inventory with reserved stock");
    }

    @Test
    @DisplayName("Should throw exception when inventory not found for deletion")
    void shouldThrowExceptionWhenInventoryNotFoundForDeletion() {
      // Given
      given(inventoryRepository.findByProductId(999L))
          .willReturn(Optional.empty());

      // When & Then
      assertThatThrownBy(() -> inventoryService.deleteInventory(999L))
          .isInstanceOf(ProductNotFoundException.class)
          .hasMessageContaining("Inventory not found for product ID: 999");
    }
  }

  @Nested
  @DisplayName("Adjust Inventory Tests")
  class AdjustInventoryTests {

    @Test
    @DisplayName("Should adjust inventory successfully")
    void shouldAdjustInventorySuccessfully() {
      // Given
      Integer newQuantity = 80;
      String reason = "Stock adjustment";
      String referenceId = "ADJ-001";

      given(inventoryRepository.findByProductIdForUpdate(100L))
          .willReturn(Optional.of(testInventory));
      given(inventoryRepository.save(any(Inventory.class)))
          .willReturn(testInventory);
      given(productService.getProductInfoSafely(100L))
          .willReturn(testProduct);

      // When
      InventoryDto result = inventoryService.adjustInventory(100L, newQuantity, reason, referenceId);

      // Then
      assertThat(result).isNotNull();
      verify(inventoryMovementRepository).save(any(InventoryMovement.class));
      verify(kafkaProducerService).publishInventoryAdjustedEvent(any(Inventory.class), eq(50), eq(reason));
    }

    @Test
    @DisplayName("Should throw exception when adjustment below reserved quantity")
    void shouldThrowExceptionWhenAdjustmentBelowReservedQuantity() {
      // Given
      Integer newQuantity = 5; // Below reserved quantity (10)
      given(inventoryRepository.findByProductIdForUpdate(100L))
          .willReturn(Optional.of(testInventory));

      // When & Then
      assertThatThrownBy(() -> inventoryService.adjustInventory(100L, newQuantity, "test", null))
          .isInstanceOf(InsufficientStockException.class)
          .hasMessageContaining("Cannot adjust quantity below reserved amount");
    }
  }

  @Nested
  @DisplayName("Synchronization Tests")
  class SynchronizationTests {

    @Test
    @DisplayName("Should return existing inventory when synchronizing")
    void shouldReturnExistingInventoryWhenSynchronizing() {
      // Given
      given(productService.getProductById(100L))
          .willReturn(Optional.of(testProduct));
      given(inventoryRepository.findByProductId(100L))
          .willReturn(Optional.of(testInventory));
      given(productService.getProductInfoSafely(100L))
          .willReturn(testProduct);

      // When
      InventoryDto result = inventoryService.synchronizeWithProductService(100L);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getProductId()).isEqualTo(100L);
      verify(inventoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should create new inventory when synchronizing non-existing product")
    void shouldCreateNewInventoryWhenSynchronizingNonExistingProduct() {
      // Given
      given(productService.getProductById(200L))
          .willReturn(Optional.of(testProduct));
      given(inventoryRepository.findByProductId(200L))
          .willReturn(Optional.empty());
      given(inventoryRepository.existsByProductId(200L))
          .willReturn(false);
      given(inventoryRepository.save(any(Inventory.class)))
          .willAnswer(invocation -> {
            Inventory inv = invocation.getArgument(0);
            inv.setId(2L);
            return inv;
          });
      given(productService.getProductInfoSafely(200L))
          .willReturn(testProduct);

      // When
      InventoryDto result = inventoryService.synchronizeWithProductService(200L);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getProductId()).isEqualTo(200L);
      assertThat(result.getQuantity()).isEqualTo(0);
      verify(inventoryRepository).save(any(Inventory.class));
    }

  }

  @Nested
  @DisplayName("Edge Cases Tests")
  class EdgeCasesTests {

    @Test
    @DisplayName("Should handle zero quantity stock check")
    void shouldHandleZeroQuantityStockCheck() {
      // Given
      given(inventoryRepository.findByProductId(100L))
          .willReturn(Optional.of(testInventory));

      // When
      boolean result = inventoryService.checkStockAvailability(100L, 0);

      // Then
      assertThat(result).isTrue(); // Zero quantity should always be available
    }

    @Test
    @DisplayName("Should handle inventory with zero available stock")
    void shouldHandleInventoryWithZeroAvailableStock() {
      // Given
      testInventory.setQuantity(10);
      testInventory.setReservedQuantity(10); // All stock is reserved
      given(inventoryRepository.findByProductId(100L))
          .willReturn(Optional.of(testInventory));

      // When
      boolean result = inventoryService.checkStockAvailability(100L, 1);

      // Then
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should handle null values in update request")
    void shouldHandleNullValuesInUpdateRequest() {
      // Given
      UpdateInventoryRequestDto partialRequest = UpdateInventoryRequestDto.builder()
          .quantity(60)
          // minStockLevel and maxStockLevel are null
          .build();

      given(inventoryRepository.findByProductIdForUpdate(100L))
          .willReturn(Optional.of(testInventory));
      given(inventoryRepository.save(any(Inventory.class)))
          .willReturn(testInventory);
      given(productService.getProductInfoSafely(100L))
          .willReturn(testProduct);

      // When
      InventoryDto result = inventoryService.updateInventory(100L, partialRequest);

      // Then
      assertThat(result).isNotNull();
      // Original min/max stock levels should be preserved
      assertThat(result.getMinStockLevel()).isEqualTo(5);
      assertThat(result.getMaxStockLevel()).isEqualTo(100);
    }
  }
}