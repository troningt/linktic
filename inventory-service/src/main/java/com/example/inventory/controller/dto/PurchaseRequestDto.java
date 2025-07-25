package com.example.inventory.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.Map;

/**
 * DTO para solicitudes de compra
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Solicitud de compra de un producto")
public class PurchaseRequestDto {

  @NotNull(message = "El ID del producto es obligatorio")
  @Schema(description = "ID del producto a comprar", example = "1", required = true)
  @JsonProperty("product_id")
  private Long productId;

  @NotNull(message = "La cantidad es obligatoria")
  @Min(value = 1, message = "La cantidad debe ser mayor a 0")
  @Schema(description = "Cantidad a comprar", example = "2", required = true)
  private Integer quantity;

  @Schema(description = "Información adicional del cliente")
  @JsonProperty("customer_info")
  private Map<String, Object> customerInfo;

  @Schema(description = "Notas adicionales sobre la compra")
  private String notes;
}
