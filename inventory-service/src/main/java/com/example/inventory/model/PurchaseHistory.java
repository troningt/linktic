package com.example.inventory.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Entidad PurchaseHistory que representa el historial de compras
 */
@Entity
@Table(name = "purchase_history", indexes = {
    @Index(name = "idx_purchase_history_product_id", columnList = "product_id"),
    @Index(name = "idx_purchase_history_purchase_date", columnList = "purchase_date"),
    @Index(name = "idx_purchase_history_status", columnList = "purchase_status"),
    @Index(name = "idx_purchase_history_purchase_id", columnList = "purchase_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"createdAt"})
public class PurchaseHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Builder.Default
  @Column(name = "purchase_id", nullable = false, updatable = false)
  private UUID purchaseId = UUID.randomUUID();

  @NotNull(message = "El ID del producto es obligatorio")
  @Column(name = "product_id", nullable = false)
  private Long productId;

  @NotNull(message = "La cantidad comprada es obligatoria")
  @Min(value = 1, message = "La cantidad comprada debe ser mayor a 0")
  @Column(name = "quantity_purchased", nullable = false)
  private Integer quantityPurchased;

  @NotNull(message = "El precio unitario es obligatorio")
  @DecimalMin(value = "0.01", message = "El precio unitario debe ser mayor a 0")
  @Digits(integer = 10, fraction = 2, message = "El precio unitario debe tener máximo 10 dígitos enteros y 2 decimales")
  @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
  private BigDecimal unitPrice;

  @NotNull(message = "El precio total es obligatorio")
  @DecimalMin(value = "0.01", message = "El precio total debe ser mayor a 0")
  @Digits(integer = 12, fraction = 2, message = "El precio total debe tener máximo 12 dígitos enteros y 2 decimales")
  @Column(name = "total_price", nullable = false, precision = 14, scale = 2)
  private BigDecimal totalPrice;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "customer_info", columnDefinition = "jsonb")
  private Map<String, Object> customerInfo;

  @NotNull(message = "El estado de la compra es obligatorio")
  @Enumerated(EnumType.STRING)
  @Builder.Default
  @Column(name = "purchase_status", nullable = false, length = 20)
  private PurchaseStatus purchaseStatus = PurchaseStatus.COMPLETED;

  @NotNull(message = "La fecha de compra es obligatoria")
  @Builder.Default
  @Column(name = "purchase_date", nullable = false)
  private LocalDateTime purchaseDate = LocalDateTime.now();

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  /**
   * Enum para el estado de la compra
   */
  public enum PurchaseStatus {
    PENDING("Pendiente"),
    COMPLETED("Completada"),
    CANCELLED("Cancelada"),
    REFUNDED("Reembolsada");

    private final String description;

    PurchaseStatus(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  /**
   * Calcula el precio total basado en cantidad y precio unitario
   */
  public void calculateTotalPrice() {
    if (quantityPurchased != null && unitPrice != null) {
      this.totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantityPurchased));
    }
  }

  /**
   * Verifica si la compra está completada
   */
  public boolean isCompleted() {
    return PurchaseStatus.COMPLETED.equals(this.purchaseStatus);
  }

  /**
   * Verifica si la compra está cancelada
   */
  public boolean isCancelled() {
    return PurchaseStatus.CANCELLED.equals(this.purchaseStatus);
  }

  /**
   * Verifica si la compra está pendiente
   */
  public boolean isPending() {
    return PurchaseStatus.PENDING.equals(this.purchaseStatus);
  }

  /**
   * Marca la compra como completada
   */
  public void markAsCompleted() {
    this.purchaseStatus = PurchaseStatus.COMPLETED;
    if (this.purchaseDate == null) {
      this.purchaseDate = LocalDateTime.now();
    }
  }

  /**
   * Marca la compra como cancelada
   */
  public void markAsCancelled() {
    this.purchaseStatus = PurchaseStatus.CANCELLED;
  }

  /**
   * Marca la compra como reembolsada
   */
  public void markAsRefunded() {
    this.purchaseStatus = PurchaseStatus.REFUNDED;
  }

  /**
   * Validación personalizada para verificar que el precio total sea correcto
   */
  @AssertTrue(message = "El precio total debe ser igual a cantidad × precio unitario")
  private boolean isTotalPriceValid() {
    if (quantityPurchased == null || unitPrice == null || totalPrice == null) {
      return true; // Las validaciones @NotNull se encargarán de estos casos
    }
    BigDecimal expectedTotal = unitPrice.multiply(BigDecimal.valueOf(quantityPurchased));
    return expectedTotal.compareTo(totalPrice) == 0;
  }
}
