package com.example.products.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para actualizar un producto existente
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Datos para actualizar un producto")
public class UpdateProductDto {

  @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
  @Schema(description = "Nombre del producto", example = "Laptop Dell XPS 13")
  private String name;

  @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
  @Digits(integer = 10, fraction = 2, message = "El precio debe tener máximo 10 dígitos enteros y 2 decimales")
  @Schema(description = "Precio del producto", example = "1299.99")
  private BigDecimal price;

  @Size(max = 500, message = "La descripción no puede exceder los 500 caracteres")
  @Schema(description = "Descripción del producto", example = "Laptop ultrabook con procesador Intel i7")
  private String description;
}
