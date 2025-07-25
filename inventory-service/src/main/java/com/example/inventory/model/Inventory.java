package com.example.inventory.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad Inventory que representa el inventario de productos
 */
@Entity
@Table(name = "inventory", indexes = {
    @Index(name = "idx_inventory_product_id", columnList = "product_id"),
    @Index(name = "idx_inventory_quantity", columnList = "quantity"),
    @Index(name = "idx_inventory_updated_at", columnList = "updated_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"createdAt", "updatedAt"})
public class Inventory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @NotNull(message = "El ID del producto es obligatorio")
  @Column(name = "product_id", nullable = false, unique = true)
  private Long productId;

  @NotNull(message = "La cantidad es obligatoria")
  @Min(value = 0, message = "La cantidad no puede ser negativa")
  @Column(name = "quantity", nullable = false)
  private Integer quantity;

  @NotNull(message = "La cantidad reservada es obligatoria")
  @Min(value = 0, message = "La cantidad reservada no puede ser negativa")
  @Builder.Default
  @Column(name = "reserved_quantity", nullable = false)
  private Integer reservedQuantity = 0;

  @NotNull(message = "El nivel mínimo de stock es obligatorio")
  @Min(value = 0, message = "El nivel mínimo de stock no puede ser negativo")
  @Builder.Default
  @Column(name = "min_stock_level", nullable = false)
  private Integer minStockLevel = 10;

  @NotNull(message = "El nivel máximo de stock es obligatorio")
  @Min(value = 1, message = "El nivel máximo de stock debe ser mayor a 0")
  @Builder.Default
  @Column(name = "max_stock_level", nullable = false)
  private Integer maxStockLevel = 1000;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Version
  @Column(name = "version")
  private Integer version;

  /**
   * Calcula la cantidad disponible (no reservada)
   */
  public Integer getAvailableQuantity() {
    return quantity - reservedQuantity;
  }

  /**
   * Verifica si hay stock suficiente disponible
   */
  public boolean hasAvailableStock(Integer requiredQuantity) {
    return getAvailableQuantity() >= requiredQuantity;
  }

  /**
   * Verifica si el stock está por debajo del nivel mínimo
   */
  public boolean isLowStock() {
    return quantity <= minStockLevel;
  }

  /**
   * Verifica si el producto está fuera de stock
   */
  public boolean isOutOfStock() {
    return quantity == 0;
  }

  /**
   * Reserva una cantidad específica de stock
   */
  public boolean reserveStock(Integer quantityToReserve) {
    if (hasAvailableStock(quantityToReserve)) {
      this.reservedQuantity += quantityToReserve;
      return true;
    }
    return false;
  }

  /**
   * Libera stock reservado
   */
  public void releaseReservedStock(Integer quantityToRelease) {
    this.reservedQuantity = Math.max(0, this.reservedQuantity - quantityToRelease);
  }

  /**
   * Confirma una compra reduciendo el stock físico y liberando la reserva
   */
  public boolean confirmPurchase(Integer quantityPurchased) {
    if (this.quantity >= quantityPurchased && this.reservedQuantity >= quantityPurchased) {
      this.quantity -= quantityPurchased;
      this.reservedQuantity -= quantityPurchased;
      return true;
    }
    return false;
  }

  /**
   * Actualiza la cantidad de stock
   */
  public void updateQuantity(Integer newQuantity) {
    this.quantity = Math.max(0, newQuantity);
  }

  /**
   * Añade stock al inventario
   */
  public void addStock(Integer quantityToAdd) {
    this.quantity += Math.max(0, quantityToAdd);
  }

  /**
   * Validación personalizada para verificar que la cantidad reservada no exceda la cantidad total
   */
  @AssertTrue(message = "La cantidad reservada no puede exceder la cantidad total")
  private boolean isReservedQuantityValid() {
    return reservedQuantity <= quantity;
  }

  /**
   * Validación personalizada para verificar que el nivel mínimo no exceda el máximo
   */
  @AssertTrue(message = "El nivel mínimo de stock no puede exceder el nivel máximo")
  private boolean areStockLevelsValid() {
    return minStockLevel <= maxStockLevel;
  }
}
