package com.example.inventory.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTO para solicitudes de actualización de inventario
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Solicitud de actualización de inventario")
public class UpdateInventoryRequestDto {

  @NotNull(message = "La cantidad es obligatoria")
  @Min(value = 0, message = "La cantidad no puede ser negativa")
  @Schema(description = "Nueva cantidad en inventario", example = "150", required = true)
  private Integer quantity;

  @Min(value = 0, message = "El nivel mínimo de stock no puede ser negativo")
  @Schema(description = "Nuevo nivel mínimo de stock", example = "20")
  @JsonProperty("min_stock_level")
  private Integer minStockLevel;

  @Min(value = 1, message = "El nivel máximo de stock debe ser mayor a 0")
  @Schema(description = "Nuevo nivel máximo de stock", example = "1000")
  @JsonProperty("max_stock_level")
  private Integer maxStockLevel;

  @Schema(description = "Razón del cambio", example = "Reposición de stock")
  private String reason;

  @Schema(description = "ID de referencia para auditoría", example = "REF-2024-001")
  @JsonProperty("reference_id")
  private String referenceId;
}
