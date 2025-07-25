package com.example.inventory.controller.dto;

import com.example.inventory.model.PurchaseHistory;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * DTO para respuestas de compra
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Respuesta de una compra realizada")
public class PurchaseResponseDto {

  @Schema(description = "ID único de la compra", example = "550e8400-e29b-41d4-a716-446655440000")
  @JsonProperty("purchase_id")
  private UUID purchaseId;

  @Schema(description = "ID del producto comprado", example = "1")
  @JsonProperty("product_id")
  private Long productId;

  @Schema(description = "Información del producto comprado")
  @JsonProperty("product_info")
  private ProductDto productInfo;

  @Schema(description = "Cantidad comprada", example = "2")
  @JsonProperty("quantity_purchased")
  private Integer quantityPurchased;

  @Schema(description = "Precio unitario", example = "1299.99")
  @JsonProperty("unit_price")
  private BigDecimal unitPrice;

  @Schema(description = "Precio total de la compra", example = "2599.98")
  @JsonProperty("total_price")
  private BigDecimal totalPrice;

  @Schema(description = "Estado de la compra", example = "COMPLETED")
  @JsonProperty("purchase_status")
  private PurchaseHistory.PurchaseStatus purchaseStatus;

  @Schema(description = "Información del cliente")
  @JsonProperty("customer_info")
  private Map<String, Object> customerInfo;

  @Schema(description = "Fecha y hora de la compra")
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  @JsonProperty("purchase_date")
  private LocalDateTime purchaseDate;

  @Schema(description = "Mensaje descriptivo del resultado")
  private String message;

  @Schema(description = "Cantidad restante en inventario después de la compra")
  @JsonProperty("remaining_stock")
  private Integer remainingStock;
}
