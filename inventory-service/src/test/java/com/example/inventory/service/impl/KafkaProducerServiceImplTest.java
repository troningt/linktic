package com.example.inventory.service.impl;

import com.example.inventory.event.InventoryEvent;
import com.example.inventory.model.Inventory;
import com.example.inventory.model.PurchaseHistory;
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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaProducerServiceImpl Tests")
class KafkaProducerServiceImplTest {

  @Mock
  private KafkaTemplate<String, Object> kafkaTemplate;

  @Mock
  private SendResult<String, Object> sendResult;

  @InjectMocks
  private KafkaProducerServiceImpl kafkaProducerService;

  private Inventory testInventory;
  private PurchaseHistory testPurchaseHistory;
  private CompletableFuture<SendResult<String, Object>> completedFuture;

  @BeforeEach
  void setUp() {
    // Set up test properties
    ReflectionTestUtils.setField(kafkaProducerService, "inventoryEventsTopic", "inventory-events");
    ReflectionTestUtils.setField(kafkaProducerService, "purchaseEventsTopic", "purchase-events");
    ReflectionTestUtils.setField(kafkaProducerService, "stockAlertsTopic", "stock-alerts");

    testInventory = Inventory.builder()
        .id(1L)
        .productId(100L)
        .quantity(50)
        .reservedQuantity(10)
        .minStockLevel(5)
        .maxStockLevel(100)
        .build();

    Map<String, Object> customerInfo = new HashMap<>();
    customerInfo.put("email", "customer@test.com");
    testPurchaseHistory = PurchaseHistory.builder()
        .id(1L)
        .purchaseId(UUID.randomUUID())
        .productId(100L)
        .quantityPurchased(5)
        .unitPrice(BigDecimal.valueOf(29.99))
        .totalPrice(BigDecimal.valueOf(149.95))
        .purchaseStatus(PurchaseHistory.PurchaseStatus.COMPLETED)
        .customerInfo(customerInfo)
        .purchaseDate(LocalDateTime.now())
        .build();

    completedFuture = CompletableFuture.completedFuture(sendResult);
  }

  @Nested
  @DisplayName("Inventory Events Tests")
  class InventoryEventsTests {

    @Test
    @DisplayName("Should publish inventory updated event successfully")
    void shouldPublishInventoryUpdatedEventSuccessfully() {
      // Given
      given(kafkaTemplate.send(eq("inventory-events"), anyString(), any(InventoryEvent.class)))
          .willReturn(completedFuture);

      // When
      kafkaProducerService.publishInventoryUpdatedEvent(testInventory);

      // Then
      ArgumentCaptor<InventoryEvent> eventCaptor = ArgumentCaptor.forClass(InventoryEvent.class);
      verify(kafkaTemplate).send(eq("inventory-events"), eq("100"), eventCaptor.capture());

      InventoryEvent capturedEvent = eventCaptor.getValue();
      assertThat(capturedEvent.getEventType()).isEqualTo(InventoryEvent.EventType.INVENTORY_UPDATED);
      assertThat(capturedEvent.getProductId()).isEqualTo(100L);
      assertThat(capturedEvent.getCurrentQuantity()).isEqualTo(50);
      assertThat(capturedEvent.getReservedQuantity()).isEqualTo(10);
      assertThat(capturedEvent.getAvailableQuantity()).isEqualTo(40);
      assertThat(capturedEvent.getMinStockLevel()).isEqualTo(5);
      assertThat(capturedEvent.getMaxStockLevel()).isEqualTo(100);
      assertThat(capturedEvent.getIsLowStock()).isFalse();
      assertThat(capturedEvent.getIsOutOfStock()).isFalse();
      assertThat(capturedEvent.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should publish inventory adjusted event successfully")
    void shouldPublishInventoryAdjustedEventSuccessfully() {
      // Given
      given(kafkaTemplate.send(eq("inventory-events"), anyString(), any(InventoryEvent.class)))
          .willReturn(completedFuture);

      Integer previousQuantity = 45;
      String reason = "Manual adjustment";

      // When
      kafkaProducerService.publishInventoryAdjustedEvent(testInventory, previousQuantity, reason);

      // Then
      ArgumentCaptor<InventoryEvent> eventCaptor = ArgumentCaptor.forClass(InventoryEvent.class);
      verify(kafkaTemplate).send(eq("inventory-events"), eq("100"), eventCaptor.capture());

      InventoryEvent capturedEvent = eventCaptor.getValue();
      assertThat(capturedEvent.getEventType()).isEqualTo(InventoryEvent.EventType.INVENTORY_ADJUSTED);
      assertThat(capturedEvent.getProductId()).isEqualTo(100L);
      assertThat(capturedEvent.getCurrentQuantity()).isEqualTo(50);
      assertThat(capturedEvent.getPreviousQuantity()).isEqualTo(45);
      assertThat(capturedEvent.getReason()).isEqualTo(reason);
    }
  }

  @Nested
  @DisplayName("Stock Alert Events Tests")
  class StockAlertEventsTests {

    @Test
    @DisplayName("Should publish low stock event successfully")
    void shouldPublishLowStockEventSuccessfully() {
      // Given
      testInventory.setQuantity(3); // Below min stock level
      given(kafkaTemplate.send(eq("stock-alerts"), anyString(), any(InventoryEvent.class)))
          .willReturn(completedFuture);

      // When
      kafkaProducerService.publishLowStockEvent(testInventory);

      // Then
      ArgumentCaptor<InventoryEvent> eventCaptor = ArgumentCaptor.forClass(InventoryEvent.class);
      verify(kafkaTemplate).send(eq("stock-alerts"), eq("100"), eventCaptor.capture());

      InventoryEvent capturedEvent = eventCaptor.getValue();
      assertThat(capturedEvent.getEventType()).isEqualTo(InventoryEvent.EventType.LOW_STOCK_ALERT);
      assertThat(capturedEvent.getProductId()).isEqualTo(100L);
      assertThat(capturedEvent.getCurrentQuantity()).isEqualTo(3);
      assertThat(capturedEvent.getMinStockLevel()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should publish out of stock event successfully")
    void shouldPublishOutOfStockEventSuccessfully() {
      // Given
      testInventory.setQuantity(0);
      given(kafkaTemplate.send(eq("stock-alerts"), anyString(), any(InventoryEvent.class)))
          .willReturn(completedFuture);

      // When
      kafkaProducerService.publishOutOfStockEvent(testInventory);

      // Then
      ArgumentCaptor<InventoryEvent> eventCaptor = ArgumentCaptor.forClass(InventoryEvent.class);
      verify(kafkaTemplate).send(eq("stock-alerts"), eq("100"), eventCaptor.capture());

      InventoryEvent capturedEvent = eventCaptor.getValue();
      assertThat(capturedEvent.getEventType()).isEqualTo(InventoryEvent.EventType.OUT_OF_STOCK_ALERT);
      assertThat(capturedEvent.getProductId()).isEqualTo(100L);
      assertThat(capturedEvent.getCurrentQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should publish stock replenishment needed event successfully")
    void shouldPublishStockReplenishmentNeededEventSuccessfully() {
      // Given
      testInventory.setQuantity(3); // Below min stock level
      given(kafkaTemplate.send(eq("stock-alerts"), anyString(), any(InventoryEvent.class)))
          .willReturn(completedFuture);

      // When
      kafkaProducerService.publishStockReplenishmentNeededEvent(testInventory);

      // Then
      ArgumentCaptor<InventoryEvent> eventCaptor = ArgumentCaptor.forClass(InventoryEvent.class);
      verify(kafkaTemplate).send(eq("stock-alerts"), eq("100"), eventCaptor.capture());

      InventoryEvent capturedEvent = eventCaptor.getValue();
      assertThat(capturedEvent.getEventType()).isEqualTo(InventoryEvent.EventType.STOCK_REPLENISHMENT_NEEDED);
      assertThat(capturedEvent.getProductId()).isEqualTo(100L);
      assertThat(capturedEvent.getCurrentQuantity()).isEqualTo(3);
      assertThat(capturedEvent.getMinStockLevel()).isEqualTo(5);

      // Verify recommended quantity calculation
      Integer expectedRecommended = Math.max(100 - 3, 5 * 2);
      assertThat(capturedEvent.getAdditionalData().get("recommended_quantity")).isEqualTo(expectedRecommended);
    }
  }

  @Nested
  @DisplayName("Stock Operations Events Tests")
  class StockOperationsEventsTests {

    @Test
    @DisplayName("Should publish stock reserved event successfully")
    void shouldPublishStockReservedEventSuccessfully() {
      // Given
      given(kafkaTemplate.send(eq("inventory-events"), anyString(), any(InventoryEvent.class)))
          .willReturn(completedFuture);

      Long productId = 100L;
      Integer quantity = 15;
      String referenceId = "REF-123";

      // When
      kafkaProducerService.publishStockReservedEvent(productId, quantity, referenceId);

      // Then
      ArgumentCaptor<InventoryEvent> eventCaptor = ArgumentCaptor.forClass(InventoryEvent.class);
      verify(kafkaTemplate).send(eq("inventory-events"), eq("100"), eventCaptor.capture());

      InventoryEvent capturedEvent = eventCaptor.getValue();
      assertThat(capturedEvent.getEventType()).isEqualTo(InventoryEvent.EventType.STOCK_RESERVED);
      assertThat(capturedEvent.getProductId()).isEqualTo(100L);
      assertThat(capturedEvent.getQuantityChange()).isEqualTo(15);
      assertThat(capturedEvent.getReferenceId()).isEqualTo("REF-123");
    }

    @Test
    @DisplayName("Should publish stock released event successfully")
    void shouldPublishStockReleasedEventSuccessfully() {
      // Given
      given(kafkaTemplate.send(eq("inventory-events"), anyString(), any(InventoryEvent.class)))
          .willReturn(completedFuture);

      Long productId = 100L;
      Integer quantity = 5;
      String referenceId = "REF-456";

      // When
      kafkaProducerService.publishStockReleasedEvent(productId, quantity, referenceId);

      // Then
      ArgumentCaptor<InventoryEvent> eventCaptor = ArgumentCaptor.forClass(InventoryEvent.class);
      verify(kafkaTemplate).send(eq("inventory-events"), eq("100"), eventCaptor.capture());

      InventoryEvent capturedEvent = eventCaptor.getValue();
      assertThat(capturedEvent.getEventType()).isEqualTo(InventoryEvent.EventType.STOCK_RELEASED);
      assertThat(capturedEvent.getProductId()).isEqualTo(100L);
      assertThat(capturedEvent.getQuantityChange()).isEqualTo(5);
      assertThat(capturedEvent.getReferenceId()).isEqualTo("REF-456");
    }
  }

  @Nested
  @DisplayName("Purchase Events Tests")
  class PurchaseEventsTests {

    @Test
    @DisplayName("Should publish purchase completed event successfully")
    void shouldPublishPurchaseCompletedEventSuccessfully() {
      // Given
      given(kafkaTemplate.send(eq("purchase-events"), anyString(), any(InventoryEvent.class)))
          .willReturn(completedFuture);

      // When
      kafkaProducerService.publishPurchaseCompletedEvent(testPurchaseHistory, testInventory);

      // Then
      ArgumentCaptor<InventoryEvent> eventCaptor = ArgumentCaptor.forClass(InventoryEvent.class);
      verify(kafkaTemplate).send(eq("purchase-events"),
          eq(testPurchaseHistory.getPurchaseId().toString()), eventCaptor.capture());

      InventoryEvent capturedEvent = eventCaptor.getValue();
      assertThat(capturedEvent.getEventType()).isEqualTo(InventoryEvent.EventType.PURCHASE_COMPLETED);
      assertThat(capturedEvent.getProductId()).isEqualTo(100L);
      assertThat(capturedEvent.getPurchaseId()).isEqualTo(testPurchaseHistory.getPurchaseId());
      assertThat(capturedEvent.getQuantityPurchased()).isEqualTo(5);
      assertThat(capturedEvent.getUnitPrice()).isEqualTo(BigDecimal.valueOf(29.99));
      assertThat(capturedEvent.getTotalPrice()).isEqualTo(BigDecimal.valueOf(149.95));
      assertThat(capturedEvent.getCurrentQuantity()).isEqualTo(50);
      assertThat(capturedEvent.getCustomerInfo()).isEqualTo("customer@test.com");
    }

    @Test
    @DisplayName("Should publish purchase cancelled event successfully")
    void shouldPublishPurchaseCancelledEventSuccessfully() {
      // Given
      testPurchaseHistory.setPurchaseStatus(PurchaseHistory.PurchaseStatus.CANCELLED);
      given(kafkaTemplate.send(eq("purchase-events"), anyString(), any(InventoryEvent.class)))
          .willReturn(completedFuture);

      // When
      kafkaProducerService.publishPurchaseCancelledEvent(testPurchaseHistory);

      // Then
      ArgumentCaptor<InventoryEvent> eventCaptor = ArgumentCaptor.forClass(InventoryEvent.class);
      verify(kafkaTemplate).send(eq("purchase-events"),
          eq(testPurchaseHistory.getPurchaseId().toString()), eventCaptor.capture());

      InventoryEvent capturedEvent = eventCaptor.getValue();
      assertThat(capturedEvent.getEventType()).isEqualTo(InventoryEvent.EventType.PURCHASE_CANCELLED);
      assertThat(capturedEvent.getProductId()).isEqualTo(100L);
      assertThat(capturedEvent.getPurchaseId()).isEqualTo(testPurchaseHistory.getPurchaseId());
      assertThat(capturedEvent.getQuantityPurchased()).isEqualTo(5);
      assertThat(capturedEvent.getReason()).isEqualTo("Purchase cancelled");
    }
  }

  @Nested
  @DisplayName("Custom Events Tests")
  class CustomEventsTests {

    @Test
    @DisplayName("Should publish custom event successfully")
    void shouldPublishCustomEventSuccessfully() {
      // Given
      InventoryEvent customEvent = InventoryEvent.builder()
          .eventType(InventoryEvent.EventType.INVENTORY_UPDATED)
          .productId(200L)
          .currentQuantity(100)
          .timestamp(LocalDateTime.now())
          .build();

      given(kafkaTemplate.send(eq("custom-topic"), anyString(), any(InventoryEvent.class)))
          .willReturn(completedFuture);

      // When
      kafkaProducerService.publishCustomEvent("custom-topic", customEvent);

      // Then
      verify(kafkaTemplate).send(eq("custom-topic"), eq("200"), eq(customEvent));
    }

    @Test
    @DisplayName("Should publish batch events successfully")
    void shouldPublishBatchEventsSuccessfully() {
      // Given
      InventoryEvent event1 = InventoryEvent.builder()
          .eventType(InventoryEvent.EventType.INVENTORY_UPDATED)
          .productId(100L)
          .currentQuantity(50)
          .timestamp(LocalDateTime.now())
          .build();

      InventoryEvent event2 = InventoryEvent.builder()
          .eventType(InventoryEvent.EventType.INVENTORY_UPDATED)
          .productId(200L)
          .currentQuantity(75)
          .timestamp(LocalDateTime.now())
          .build();

      List<InventoryEvent> events = Arrays.asList(event1, event2);

      given(kafkaTemplate.send(eq("batch-topic"), anyString(), any(InventoryEvent.class)))
          .willReturn(completedFuture);

      // When
      kafkaProducerService.publishBatchEvents("batch-topic", events);

      // Then
      verify(kafkaTemplate).send(eq("batch-topic"), eq("100"), eq(event1));
      verify(kafkaTemplate).send(eq("batch-topic"), eq("200"), eq(event2));
    }
  }

  @Nested
  @DisplayName("Error Handling Tests")
  class ErrorHandlingTests {

    @Test
    @DisplayName("Should handle kafka send exception gracefully")
    void shouldHandleKafkaSendExceptionGracefully() {
      // Given
      given(kafkaTemplate.send(eq("inventory-events"), anyString(), any(InventoryEvent.class)))
          .willThrow(new RuntimeException("Kafka connection error"));

      // When & Then - Should not throw exception
      assertThatCode(() -> kafkaProducerService.publishInventoryUpdatedEvent(testInventory))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle failed kafka future gracefully")
    void shouldHandleFailedKafkaFutureGracefully() {
      // Given
      CompletableFuture<SendResult<String, Object>> failedFuture = new CompletableFuture<>();
      failedFuture.completeExceptionally(new RuntimeException("Send failed"));

      given(kafkaTemplate.send(eq("inventory-events"), anyString(), any(InventoryEvent.class)))
          .willReturn(failedFuture);

      // When & Then - Should not throw exception
      assertThatCode(() -> kafkaProducerService.publishInventoryUpdatedEvent(testInventory))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle null inventory gracefully")
    void shouldHandleNullInventoryGracefully() {
      // When & Then - Should not throw exception
      assertThatCode(() -> kafkaProducerService.publishInventoryUpdatedEvent(null))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle null purchase history gracefully")
    void shouldHandleNullPurchaseHistoryGracefully() {
      // When & Then - Should not throw exception
      assertThatCode(() -> kafkaProducerService.publishPurchaseCompletedEvent(null, testInventory))
          .doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("Event Key Generation Tests")
  class EventKeyGenerationTests {

    @Test
    @DisplayName("Should use product ID as key for inventory events")
    void shouldUseProductIdAsKeyForInventoryEvents() {
      // Given
      given(kafkaTemplate.send(eq("inventory-events"), anyString(), any(InventoryEvent.class)))
          .willReturn(completedFuture);

      // When
      kafkaProducerService.publishInventoryUpdatedEvent(testInventory);

      // Then
      verify(kafkaTemplate).send(eq("inventory-events"), eq("100"), any(InventoryEvent.class));
    }

    @Test
    @DisplayName("Should use purchase ID as key for purchase events")
    void shouldUsePurchaseIdAsKeyForPurchaseEvents() {
      // Given
      given(kafkaTemplate.send(eq("purchase-events"), anyString(), any(InventoryEvent.class)))
          .willReturn(completedFuture);

      // When
      kafkaProducerService.publishPurchaseCompletedEvent(testPurchaseHistory, testInventory);

      // Then
      verify(kafkaTemplate).send(eq("purchase-events"),
          eq(testPurchaseHistory.getPurchaseId().toString()), any(InventoryEvent.class));
    }

    @Test
    @DisplayName("Should use event ID as key when product ID is null")
    void shouldUseEventIdAsKeyWhenProductIdIsNull() {
      // Given
      InventoryEvent eventWithNullProductId = InventoryEvent.builder()
          .eventType(InventoryEvent.EventType.INVENTORY_UPDATED)
          .productId(null)
          .currentQuantity(50)
          .timestamp(LocalDateTime.now())
          .build();

      given(kafkaTemplate.send(eq("custom-topic"), anyString(), any(InventoryEvent.class)))
          .willReturn(completedFuture);

      // When
      kafkaProducerService.publishCustomEvent("custom-topic", eventWithNullProductId);

      // Then
      ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
      verify(kafkaTemplate).send(eq("custom-topic"), keyCaptor.capture(), eq(eventWithNullProductId));

      // Key should be the event ID (UUID)
      String capturedKey = keyCaptor.getValue();
      assertThat(capturedKey).isNotNull();
      assertThat(capturedKey).isEqualTo(eventWithNullProductId.getEventId());
    }
  }

  @Nested
  @DisplayName("Event Content Validation Tests")
  class EventContentValidationTests {

    @Test
    @DisplayName("Should create complete inventory updated event")
    void shouldCreateCompleteInventoryUpdatedEvent() {
      // Given
      given(kafkaTemplate.send(eq("inventory-events"), anyString(), any(InventoryEvent.class)))
          .willReturn(completedFuture);

      // When
      kafkaProducerService.publishInventoryUpdatedEvent(testInventory);

      // Then
      ArgumentCaptor<InventoryEvent> eventCaptor = ArgumentCaptor.forClass(InventoryEvent.class);
      verify(kafkaTemplate).send(eq("inventory-events"), anyString(), eventCaptor.capture());

      InventoryEvent event = eventCaptor.getValue();
      assertThat(event.getEventId()).isNotNull();
      assertThat(event.getEventType()).isNotNull();
      assertThat(event.getProductId()).isNotNull();
      assertThat(event.getCurrentQuantity()).isNotNull();
      assertThat(event.getReservedQuantity()).isNotNull();
      assertThat(event.getAvailableQuantity()).isNotNull();
      assertThat(event.getMinStockLevel()).isNotNull();
      assertThat(event.getMaxStockLevel()).isNotNull();
      assertThat(event.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should create complete purchase completed event")
    void shouldCreateCompletePurchaseCompletedEvent() {
      // Given
      given(kafkaTemplate.send(eq("purchase-events"), anyString(), any(InventoryEvent.class)))
          .willReturn(completedFuture);

      // When
      kafkaProducerService.publishPurchaseCompletedEvent(testPurchaseHistory, testInventory);

      // Then
      ArgumentCaptor<InventoryEvent> eventCaptor = ArgumentCaptor.forClass(InventoryEvent.class);
      verify(kafkaTemplate).send(eq("purchase-events"), anyString(), eventCaptor.capture());

      InventoryEvent event = eventCaptor.getValue();
      assertThat(event.getEventId()).isNotNull();
      assertThat(event.getEventType()).isEqualTo(InventoryEvent.EventType.PURCHASE_COMPLETED);
      assertThat(event.getProductId()).isEqualTo(testPurchaseHistory.getProductId());
      assertThat(event.getPurchaseId()).isEqualTo(testPurchaseHistory.getPurchaseId());
      assertThat(event.getQuantityPurchased()).isEqualTo(testPurchaseHistory.getQuantityPurchased());
      assertThat(event.getUnitPrice()).isEqualTo(testPurchaseHistory.getUnitPrice());
      assertThat(event.getTotalPrice()).isEqualTo(testPurchaseHistory.getTotalPrice());
      assertThat(event.getCurrentQuantity()).isEqualTo(testInventory.getQuantity());
      assertThat(event.getCustomerInfo()).isEqualTo(testPurchaseHistory.getCustomerInfo());
      assertThat(event.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should calculate recommended quantity correctly for replenishment event")
    void shouldCalculateRecommendedQuantityCorrectlyForReplenishmentEvent() {
      // Given
      testInventory.setQuantity(2);
      testInventory.setMinStockLevel(10);
      testInventory.setMaxStockLevel(100);

      given(kafkaTemplate.send(eq("stock-alerts"), anyString(), any(InventoryEvent.class)))
          .willReturn(completedFuture);

      // When
      kafkaProducerService.publishStockReplenishmentNeededEvent(testInventory);

      // Then
      ArgumentCaptor<InventoryEvent> eventCaptor = ArgumentCaptor.forClass(InventoryEvent.class);
      verify(kafkaTemplate).send(eq("stock-alerts"), anyString(), eventCaptor.capture());

      InventoryEvent event = eventCaptor.getValue();

      // Expected: max(100 - 2, 10 * 2) = max(98, 20) = 98
      assertThat(event.getAdditionalData().get("recommended_quantity")).isEqualTo(98);
    }

    @Test
    @DisplayName("Should use minimum recommended quantity when max stock level difference is small")
    void shouldUseMinimumRecommendedQuantityWhenMaxStockLevelDifferenceIsSmall() {
      // Given
      testInventory.setQuantity(95);
      testInventory.setMinStockLevel(10);
      testInventory.setMaxStockLevel(100);

      given(kafkaTemplate.send(eq("stock-alerts"), anyString(), any(InventoryEvent.class)))
          .willReturn(completedFuture);

      // When
      kafkaProducerService.publishStockReplenishmentNeededEvent(testInventory);

      // Then
      ArgumentCaptor<InventoryEvent> eventCaptor = ArgumentCaptor.forClass(InventoryEvent.class);
      verify(kafkaTemplate).send(eq("stock-alerts"), anyString(), eventCaptor.capture());

      InventoryEvent event = eventCaptor.getValue();

      // Expected: max(100 - 95, 10 * 2) = max(5, 20) = 20
      assertThat(event.getAdditionalData().get("recommended_quantity")).isEqualTo(20);
    }
  }

  @Nested
  @DisplayName("Topic Configuration Tests")
  class TopicConfigurationTests {

    @Test
    @DisplayName("Should use correct topic for inventory events")
    void shouldUseCorrectTopicForInventoryEvents() {
      // Given
      given(kafkaTemplate.send(eq("inventory-events"), anyString(), any(InventoryEvent.class)))
          .willReturn(completedFuture);

      // When
      kafkaProducerService.publishInventoryUpdatedEvent(testInventory);
      kafkaProducerService.publishInventoryAdjustedEvent(testInventory, 45, "test");
      kafkaProducerService.publishStockReservedEvent(100L, 10, null);
      kafkaProducerService.publishStockReleasedEvent(100L, 5, null);

      // Then
      verify(kafkaTemplate, times(4)).send(eq("inventory-events"), anyString(), any(InventoryEvent.class));
    }

    @Test
    @DisplayName("Should use correct topic for purchase events")
    void shouldUseCorrectTopicForPurchaseEvents() {
      // Given
      given(kafkaTemplate.send(eq("purchase-events"), anyString(), any(InventoryEvent.class)))
          .willReturn(completedFuture);

      // When
      kafkaProducerService.publishPurchaseCompletedEvent(testPurchaseHistory, testInventory);
      kafkaProducerService.publishPurchaseCancelledEvent(testPurchaseHistory);

      // Then
      verify(kafkaTemplate, times(2)).send(eq("purchase-events"), anyString(), any(InventoryEvent.class));
    }

    @Test
    @DisplayName("Should use correct topic for stock alerts")
    void shouldUseCorrectTopicForStockAlerts() {
      // Given
      given(kafkaTemplate.send(eq("stock-alerts"), anyString(), any(InventoryEvent.class)))
          .willReturn(completedFuture);

      // When
      kafkaProducerService.publishLowStockEvent(testInventory);
      kafkaProducerService.publishOutOfStockEvent(testInventory);
      kafkaProducerService.publishStockReplenishmentNeededEvent(testInventory);

      // Then
      verify(kafkaTemplate, times(3)).send(eq("stock-alerts"), anyString(), any(InventoryEvent.class));
    }
  }
}