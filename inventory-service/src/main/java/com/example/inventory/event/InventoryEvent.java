package com.example.inventory.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Evento de inventario para Kafka
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryEvent {

  @JsonProperty("event_type")
  private String eventType;

  @JsonProperty("event_id")
  @Builder.Default
  private String eventId = UUID.randomUUID().toString();

  @JsonProperty("product_id")
  private Long productId;

  @JsonProperty("product_name")
  private String productName;

  @JsonProperty("current_quantity")
  private Integer currentQuantity;

  @JsonProperty("previous_quantity")
  private Integer previousQuantity;

  @JsonProperty("quantity_change")
  private Integer quantityChange;

  @JsonProperty("reserved_quantity")
  private Integer reservedQuantity;

  @JsonProperty("available_quantity")
  private Integer availableQuantity;

  @JsonProperty("min_stock_level")
  private Integer minStockLevel;

  @JsonProperty("max_stock_level")
  private Integer maxStockLevel;

  @JsonProperty("is_low_stock")
  private Boolean isLowStock;

  @JsonProperty("is_out_of_stock")
  private Boolean isOutOfStock;

  @JsonProperty("unit_price")
  private BigDecimal unitPrice;

  @JsonProperty("purchase_id")
  private UUID purchaseId;

  @JsonProperty("quantity_purchased")
  private Integer quantityPurchased;

  @JsonProperty("total_price")
  private BigDecimal totalPrice;

  @JsonProperty("reference_id")
  private String referenceId;

  private String reason;

  @JsonProperty("customer_info")
  private Map<String, Object> customerInfo;

  @JsonProperty("additional_data")
  private Map<String, Object> additionalData;

  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  private LocalDateTime timestamp;

  @JsonProperty("service_name")
  @Builder.Default
  private String serviceName = "inventory-service";

  @JsonProperty("service_version")
  @Builder.Default
  private String serviceVersion = "1.0.0";

  /**
   * Tipos de eventos de inventario
   */
  public static class EventType {
    public static final String INVENTORY_UPDATED = "INVENTORY_UPDATED";
    public static final String LOW_STOCK_ALERT = "LOW_STOCK_ALERT";
    public static final String OUT_OF_STOCK_ALERT = "OUT_OF_STOCK_ALERT";
    public static final String PURCHASE_COMPLETED = "PURCHASE_COMPLETED";
    public static final String PURCHASE_CANCELLED = "PURCHASE_CANCELLED";
    public static final String STOCK_RESERVED = "STOCK_RESERVED";
    public static final String STOCK_RELEASED = "STOCK_RELEASED";
    public static final String INVENTORY_ADJUSTED = "INVENTORY_ADJUSTED";
    public static final String STOCK_REPLENISHMENT_NEEDED = "STOCK_REPLENISHMENT_NEEDED";
    public static final String PURCHASE_FAILED = "PURCHASE_FAILED";
    public static final String INVENTORY_CREATED = "INVENTORY_CREATED";
    public static final String INVENTORY_DELETED = "INVENTORY_DELETED";
  }

  /**
   * Crea un evento de inventario actualizado
   */
  public static InventoryEvent createInventoryUpdatedEvent(Long productId, String productName,
                                                           Integer currentQuantity, Integer previousQuantity,
                                                           Integer reservedQuantity, Integer minStockLevel,
                                                           Integer maxStockLevel) {
    return InventoryEvent.builder()
        .eventType(EventType.INVENTORY_UPDATED)
        .productId(productId)
        .productName(productName)
        .currentQuantity(currentQuantity)
        .previousQuantity(previousQuantity)
        .quantityChange(currentQuantity - previousQuantity)
        .reservedQuantity(reservedQuantity)
        .availableQuantity(currentQuantity - reservedQuantity)
        .minStockLevel(minStockLevel)
        .maxStockLevel(maxStockLevel)
        .isLowStock(currentQuantity <= minStockLevel)
        .isOutOfStock(currentQuantity == 0)
        .timestamp(LocalDateTime.now())
        .build();
  }

  /**
   * Crea un evento de stock bajo
   */
  public static InventoryEvent createLowStockEvent(Long productId, String productName,
                                                   Integer currentQuantity, Integer minStockLevel) {
    return InventoryEvent.builder()
        .eventType(EventType.LOW_STOCK_ALERT)
        .productId(productId)
        .productName(productName)
        .currentQuantity(currentQuantity)
        .minStockLevel(minStockLevel)
        .isLowStock(true)
        .reason("Stock level is below minimum threshold")
        .timestamp(LocalDateTime.now())
        .build();
  }

  /**
   * Crea un evento de producto fuera de stock
   */
  public static InventoryEvent createOutOfStockEvent(Long productId, String productName) {
    return InventoryEvent.builder()
        .eventType(EventType.OUT_OF_STOCK_ALERT)
        .productId(productId)
        .productName(productName)
        .currentQuantity(0)
        .availableQuantity(0)
        .isOutOfStock(true)
        .reason("Product is out of stock")
        .timestamp(LocalDateTime.now())
        .build();
  }

  /**
   * Crea un evento de compra completada
   */
  public static InventoryEvent createPurchaseCompletedEvent(Long productId, String productName,
                                                            UUID purchaseId, Integer quantityPurchased,
                                                            BigDecimal unitPrice, BigDecimal totalPrice,
                                                            Integer remainingStock,
                                                            Map<String, Object> customerInfo) {
    return InventoryEvent.builder()
        .eventType(EventType.PURCHASE_COMPLETED)
        .productId(productId)
        .productName(productName)
        .purchaseId(purchaseId)
        .quantityPurchased(quantityPurchased)
        .unitPrice(unitPrice)
        .totalPrice(totalPrice)
        .currentQuantity(remainingStock)
        .quantityChange(-quantityPurchased)
        .customerInfo(customerInfo)
        .reason("Purchase completed successfully")
        .timestamp(LocalDateTime.now())
        .build();
  }

  /**
   * Crea un evento de compra cancelada
   */
  public static InventoryEvent createPurchaseCancelledEvent(Long productId, UUID purchaseId,
                                                            Integer quantityRequested, String reason) {
    return InventoryEvent.builder()
        .eventType(EventType.PURCHASE_CANCELLED)
        .productId(productId)
        .purchaseId(purchaseId)
        .quantityPurchased(quantityRequested)
        .reason(reason != null ? reason : "Purchase cancelled")
        .timestamp(LocalDateTime.now())
        .build();
  }

  /**
   * Crea un evento de stock reservado
   */
  public static InventoryEvent createStockReservedEvent(Long productId, Integer quantity,
                                                        String referenceId) {
    return InventoryEvent.builder()
        .eventType(EventType.STOCK_RESERVED)
        .productId(productId)
        .quantityChange(quantity)
        .referenceId(referenceId)
        .reason("Stock reserved for purchase")
        .timestamp(LocalDateTime.now())
        .build();
  }

  /**
   * Crea un evento de liberación de stock
   */
  public static InventoryEvent createStockReleasedEvent(Long productId, Integer quantity,
                                                        String referenceId) {
    return InventoryEvent.builder()
        .eventType(EventType.STOCK_RELEASED)
        .productId(productId)
        .quantityChange(quantity)
        .referenceId(referenceId)
        .reason("Reserved stock released")
        .timestamp(LocalDateTime.now())
        .build();
  }

  /**
   * Crea un evento de ajuste de inventario
   */
  public static InventoryEvent createInventoryAdjustedEvent(Long productId, String productName,
                                                            Integer currentQuantity, Integer previousQuantity,
                                                            String reason, String referenceId) {
    return InventoryEvent.builder()
        .eventType(EventType.INVENTORY_ADJUSTED)
        .productId(productId)
        .productName(productName)
        .currentQuantity(currentQuantity)
        .previousQuantity(previousQuantity)
        .quantityChange(currentQuantity - previousQuantity)
        .reason(reason)
        .referenceId(referenceId)
        .timestamp(LocalDateTime.now())
        .build();
  }

  /**
   * Crea un evento de necesidad de reposición
   */
  public static InventoryEvent createStockReplenishmentNeededEvent(Long productId, String productName,
                                                                   Integer currentQuantity,
                                                                   Integer minStockLevel,
                                                                   Integer recommendedQuantity) {
    return InventoryEvent.builder()
        .eventType(EventType.STOCK_REPLENISHMENT_NEEDED)
        .productId(productId)
        .productName(productName)
        .currentQuantity(currentQuantity)
        .minStockLevel(minStockLevel)
        .isLowStock(true)
        .reason("Stock replenishment needed")
        .additionalData(Map.of("recommended_quantity", recommendedQuantity))
        .timestamp(LocalDateTime.now())
        .build();
  }
}