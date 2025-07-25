package com.example.inventory.controller.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO para representar información de inventario
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Información de inventario de un producto")
public class InventoryDto {

  @Schema(description = "ID único del inventario", example = "1")
  private Long id;

  @NotNull(message = "El ID del producto es obligatorio")
  @Schema(description = "ID del producto", example = "1", required = true)
  @JsonProperty("product_id")
  private Long productId;

  @NotNull(message = "La cantidad es obligatoria")
  @Min(value = 0, message = "La cantidad no puede ser negativa")
  @Schema(description = "Cantidad disponible en inventario", example = "100", required = true)
  private Integer quantity;

  @Min(value = 0, message = "La cantidad reservada no puede ser negativa")
  @Schema(description = "Cantidad reservada del producto", example = "10")
  @JsonProperty("reserved_quantity")
  private Integer reservedQuantity;

  @Schema(description = "Cantidad disponible para venta", example = "90")
  @JsonProperty("available_quantity")
  private Integer availableQuantity;

  @Min(value = 0, message = "El nivel mínimo de stock no puede ser negativo")
  @Schema(description = "Nivel mínimo de stock recomendado", example = "20")
  @JsonProperty("min_stock_level")
  private Integer minStockLevel;

  @Min(value = 1, message = "El nivel máximo de stock debe ser mayor a 0")
  @Schema(description = "Nivel máximo de stock permitido", example = "1000")
  @JsonProperty("max_stock_level")
  private Integer maxStockLevel;

  @Schema(description = "Indica si el producto está con stock bajo")
  @JsonProperty("is_low_stock")
  private Boolean isLowStock;

  @Schema(description = "Indica si el producto está fuera de stock")
  @JsonProperty("is_out_of_stock")
  private Boolean isOutOfStock;

  @Schema(description = "Información del producto asociado")
  @JsonProperty("product_info")
  private ProductDto productInfo;

  @Schema(description = "Fecha de creación del registro")
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  @JsonProperty("created_at")
  private LocalDateTime createdAt;

  @Schema(description = "Fecha de última actualización")
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  @JsonProperty("updated_at")
  private LocalDateTime updatedAt;
}
