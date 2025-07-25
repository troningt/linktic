package com.example.inventory.controller.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para representar información básica de un producto
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Información básica de un producto")
public class ProductDto {

  @Schema(description = "ID único del producto", example = "1")
  private Long id;

  @Schema(description = "Nombre del producto", example = "Laptop Gaming")
  private String name;

  @Schema(description = "Precio del producto", example = "1299.99")
  private BigDecimal price;

  @Schema(description = "Descripción del producto", example = "Laptop de alto rendimiento")
  private String description;

  @Schema(description = "Indica si el producto está activo", example = "true")
  private Boolean active;

  @Schema(description = "Fecha de creación del producto")
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  @JsonProperty("created_at")
  private LocalDateTime createdAt;

  @Schema(description = "Fecha de última actualización del producto")
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  @JsonProperty("updated_at")
  private LocalDateTime updatedAt;
}
