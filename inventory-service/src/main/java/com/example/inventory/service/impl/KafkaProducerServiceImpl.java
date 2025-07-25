package com.example.inventory.service.impl;

import com.example.inventory.model.Inventory;
import com.example.inventory.model.PurchaseHistory;
import com.example.inventory.event.InventoryEvent;
import com.example.inventory.service.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * Implementación del servicio de publicación de eventos Kafka para inventario
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerServiceImpl implements KafkaProducerService {

  private final KafkaTemplate<String, Object> kafkaTemplate;

  @Value("${app.kafka.topics.inventory-events}")
  private String inventoryEventsTopic;

  @Value("${app.kafka.topics.purchase-events}")
  private String purchaseEventsTopic;

  @Value("${app.kafka.topics.stock-alerts}")
  private String stockAlertsTopic;

  @Override
  public void publishInventoryUpdatedEvent(Inventory inventory) {
    InventoryEvent event = InventoryEvent.builder()
        .eventType(InventoryEvent.EventType.INVENTORY_UPDATED)
        .productId(inventory.getProductId())
        .currentQuantity(inventory.getQuantity())
        .reservedQuantity(inventory.getReservedQuantity())
        .availableQuantity(inventory.getAvailableQuantity())
        .minStockLevel(inventory.getMinStockLevel())
        .maxStockLevel(inventory.getMaxStockLevel())
        .isLowStock(inventory.isLowStock())
        .isOutOfStock(inventory.isOutOfStock())
        .timestamp(LocalDateTime.now())
        .build();

    publishEvent(inventoryEventsTopic, event,
        "Inventory updated event published for product ID: " + inventory.getProductId());
  }

  @Override
  public void publishLowStockEvent(Inventory inventory) {
    InventoryEvent event = InventoryEvent.createLowStockEvent(
        inventory.getProductId(),
        null, // productName se puede obtener del servicio de productos si es necesario
        inventory.getQuantity(),
        inventory.getMinStockLevel()
    );

    publishEvent(stockAlertsTopic, event,
        "Low stock alert published for product ID: " + inventory.getProductId());
  }

  @Override
  public void publishOutOfStockEvent(Inventory inventory) {
    InventoryEvent event = InventoryEvent.createOutOfStockEvent(
        inventory.getProductId(),
        null // productName se puede obtener del servicio de productos si es necesario
    );

    publishEvent(stockAlertsTopic, event,
        "Out of stock alert published for product ID: " + inventory.getProductId());
  }

  @Override
  public void publishPurchaseCompletedEvent(PurchaseHistory purchaseHistory, Inventory inventory) {
    InventoryEvent event = InventoryEvent.createPurchaseCompletedEvent(
        purchaseHistory.getProductId(),
        null, // productName
        purchaseHistory.getPurchaseId(),
        purchaseHistory.getQuantityPurchased(),
        purchaseHistory.getUnitPrice(),
        purchaseHistory.getTotalPrice(),
        inventory.getQuantity(),
        purchaseHistory.getCustomerInfo()
    );

    publishEvent(purchaseEventsTopic, event,
        "Purchase completed event published for purchase ID: " + purchaseHistory.getPurchaseId());
  }

  @Override
  public void publishPurchaseCancelledEvent(PurchaseHistory purchaseHistory) {
    InventoryEvent event = InventoryEvent.createPurchaseCancelledEvent(
        purchaseHistory.getProductId(),
        purchaseHistory.getPurchaseId(),
        purchaseHistory.getQuantityPurchased(),
        "Purchase cancelled"
    );

    publishEvent(purchaseEventsTopic, event,
        "Purchase cancelled event published for purchase ID: " + purchaseHistory.getPurchaseId());
  }

  @Override
  public void publishStockReservedEvent(Long productId, Integer quantity, String referenceId) {
    InventoryEvent event = InventoryEvent.createStockReservedEvent(
        productId, quantity, referenceId);

    publishEvent(inventoryEventsTopic, event,
        "Stock reserved event published for product ID: " + productId);
  }

  @Override
  public void publishStockReleasedEvent(Long productId, Integer quantity, String referenceId) {
    InventoryEvent event = InventoryEvent.createStockReleasedEvent(
        productId, quantity, referenceId);

    publishEvent(inventoryEventsTopic, event,
        "Stock released event published for product ID: " + productId);
  }

  @Override
  public void publishInventoryAdjustedEvent(Inventory inventory, Integer previousQuantity, String reason) {
    InventoryEvent event = InventoryEvent.createInventoryAdjustedEvent(
        inventory.getProductId(),
        null, // productName
        inventory.getQuantity(),
        previousQuantity,
        reason,
        null // referenceId
    );

    publishEvent(inventoryEventsTopic, event,
        "Inventory adjusted event published for product ID: " + inventory.getProductId());
  }

  @Override
  public void publishStockReplenishmentNeededEvent(Inventory inventory) {
    // Calcular cantidad recomendada para reposición
    Integer recommendedQuantity = Math.max(
        inventory.getMaxStockLevel() - inventory.getQuantity(),
        inventory.getMinStockLevel() * 2
    );

    InventoryEvent event = InventoryEvent.createStockReplenishmentNeededEvent(
        inventory.getProductId(),
        null, // productName
        inventory.getQuantity(),
        inventory.getMinStockLevel(),
        recommendedQuantity
    );

    publishEvent(stockAlertsTopic, event,
        "Stock replenishment needed event published for product ID: " + inventory.getProductId());
  }

  /**
   * Método privado para publicar eventos en Kafka
   */
  private void publishEvent(String topic, InventoryEvent event, String logMessage) {
    try {
      String key = event.getProductId() != null ? event.getProductId().toString() : event.getEventId();

      CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, event);

      future.whenComplete((result, ex) -> {
        if (ex == null) {
          log.info("{} - Topic: {}, Offset: {}, Partition: {}, Key: {}",
              logMessage,
              topic,
              result.getRecordMetadata().offset(),
              result.getRecordMetadata().partition(),
              key);
        } else {
          log.error("Failed to publish event to topic {} for product ID: {} - Error: {}",
              topic, event.getProductId(), ex.getMessage(), ex);
        }
      });

    } catch (Exception e) {
      log.error("Exception publishing event to topic {} for product ID: {} - Error: {}",
          topic, event.getProductId(), e.getMessage(), e);
    }
  }

  /**
   * Publica un evento personalizado
   */
  public void publishCustomEvent(String topic, InventoryEvent event) {
    publishEvent(topic, event, "Custom event published");
  }

  /**
   * Publica múltiples eventos en batch (para optimización futura)
   */
  public void publishBatchEvents(String topic, java.util.List<InventoryEvent> events) {
    events.forEach(event -> publishEvent(topic, event, "Batch event published"));
  }
}