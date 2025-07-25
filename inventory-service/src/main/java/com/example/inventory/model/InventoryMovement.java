package com.example.inventory.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad InventoryMovement para auditoría de movimientos de inventario
 */
@Entity
@Table(name = "inventory_movements", indexes = {
    @Index(name = "idx_inventory_movements_product_id", columnList = "product_id"),
    @Index(name = "idx_inventory_movements_type", columnList = "movement_type"),
    @Index(name = "idx_inventory_movements_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"createdAt"})
public class InventoryMovement {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @NotNull(message = "El ID del producto es obligatorio")
  @Column(name = "product_id", nullable = false)
  private Long productId;

  @NotNull(message = "El tipo de movimiento es obligatorio")
  @Enumerated(EnumType.STRING)
  @Column(name = "movement_type", nullable = false, length = 20)
  private MovementType movementType;

  @NotNull(message = "El cambio de cantidad es obligatorio")
  @Column(name = "quantity_change", nullable = false)
  private Integer quantityChange;

  @NotNull(message = "La cantidad anterior es obligatoria")
  @Min(value = 0, message = "La cantidad anterior no puede ser negativa")
  @Column(name = "previous_quantity", nullable = false)
  private Integer previousQuantity;

  @NotNull(message = "La nueva cantidad es obligatoria")
  @Min(value = 0, message = "La nueva cantidad no puede ser negativa")
  @Column(name = "new_quantity", nullable = false)
  private Integer newQuantity;

  @Size(max = 100, message = "El ID de referencia no puede exceder los 100 caracteres")
  @Column(name = "reference_id", length = 100)
  private String referenceId;

  @Size(max = 200, message = "La razón no puede exceder los 200 caracteres")
  @Column(name = "reason", length = 200)
  private String reason;

  @Size(max = 100, message = "El creador no puede exceder los 100 caracteres")
  @Builder.Default
  @Column(name = "created_by", length = 100)
  private String createdBy = "SYSTEM";

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  /**
   * Enum para los tipos de movimiento de inventario
   */
  public enum MovementType {
    STOCK_IN("Entrada de stock"),
    STOCK_OUT("Salida de stock"),
    ADJUSTMENT("Ajuste de inventario"),
    RESERVATION("Reserva de stock"),
    RELEASE("Liberación de reserva"),
    PURCHASE("Compra"),
    RETURN("Devolución"),
    DAMAGED("Producto dañado"),
    EXPIRED("Producto vencido"),
    TRANSFER("Transferencia"),
    INITIAL_STOCK("Stock inicial");

    private final String description;

    MovementType(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  /**
   * Verifica si el movimiento es una entrada de stock
   */
  public boolean isStockInMovement() {
    return quantityChange > 0;
  }

  /**
   * Verifica si el movimiento es una salida de stock
   */
  public boolean isStockOutMovement() {
    return quantityChange < 0;
  }

  /**
   * Verifica si el movimiento es un ajuste
   */
  public boolean isAdjustment() {
    return MovementType.ADJUSTMENT.equals(this.movementType);
  }

  /**
   * Verifica si el movimiento está relacionado con una compra
   */
  public boolean isPurchaseRelated() {
    return MovementType.PURCHASE.equals(this.movementType) ||
        MovementType.STOCK_OUT.equals(this.movementType);
  }

  /**
   * Verifica si el movimiento es una reserva
   */
  public boolean isReservation() {
    return MovementType.RESERVATION.equals(this.movementType);
  }

  /**
   * Verifica si el movimiento es una liberación de reserva
   */
  public boolean isRelease() {
    return MovementType.RELEASE.equals(this.movementType);
  }

  /**
   * Crea un movimiento de entrada de stock
   */
  public static InventoryMovement createStockInMovement(Long productId, Integer quantityAdded,
                                                        Integer previousQuantity, String reason,
                                                        String referenceId) {
    return InventoryMovement.builder()
        .productId(productId)
        .movementType(MovementType.STOCK_IN)
        .quantityChange(quantityAdded)
        .previousQuantity(previousQuantity)
        .newQuantity(previousQuantity + quantityAdded)
        .reason(reason)
        .referenceId(referenceId)
        .build();
  }

  /**
   * Crea un movimiento de salida de stock
   */
  public static InventoryMovement createStockOutMovement(Long productId, Integer quantityRemoved,
                                                         Integer previousQuantity, String reason,
                                                         String referenceId) {
    return InventoryMovement.builder()
        .productId(productId)
        .movementType(MovementType.STOCK_OUT)
        .quantityChange(-quantityRemoved)
        .previousQuantity(previousQuantity)
        .newQuantity(previousQuantity - quantityRemoved)
        .reason(reason)
        .referenceId(referenceId)
        .build();
  }

  /**
   * Crea un movimiento de ajuste de inventario
   */
  public static InventoryMovement createAdjustmentMovement(Long productId, Integer previousQuantity,
                                                           Integer newQuantity, String reason,
                                                           String referenceId) {
    return InventoryMovement.builder()
        .productId(productId)
        .movementType(MovementType.ADJUSTMENT)
        .quantityChange(newQuantity - previousQuantity)
        .previousQuantity(previousQuantity)
        .newQuantity(newQuantity)
        .reason(reason)
        .referenceId(referenceId)
        .build();
  }

  /**
   * Crea un movimiento de compra
   */
  public static InventoryMovement createPurchaseMovement(Long productId, Integer quantityPurchased,
                                                         Integer previousQuantity, String purchaseId) {
    return InventoryMovement.builder()
        .productId(productId)
        .movementType(MovementType.PURCHASE)
        .quantityChange(-quantityPurchased)
        .previousQuantity(previousQuantity)
        .newQuantity(previousQuantity - quantityPurchased)
        .reason("Compra realizada")
        .referenceId(purchaseId)
        .build();
  }

  /**
   * Validación personalizada para verificar que la nueva cantidad sea consistente
   */
  @AssertTrue(message = "La nueva cantidad debe ser igual a cantidad anterior + cambio de cantidad")
  private boolean isNewQuantityValid() {
    if (previousQuantity == null || quantityChange == null || newQuantity == null) {
      return true;
    }
    return newQuantity.equals(previousQuantity + quantityChange);
  }
}
