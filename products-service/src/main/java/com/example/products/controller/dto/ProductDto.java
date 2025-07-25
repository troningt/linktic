package com.example.products.controller.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para Product - Request/Response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Datos del producto")
public class ProductDto {

  @Schema(description = "ID único del producto", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private Long id;

  @NotBlank(message = "El nombre del producto es obligatorio")
  @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
  @Schema(description = "Nombre del producto", example = "Laptop Dell XPS 13", required = true)
  private String name;

  @NotNull(message = "El precio es obligatorio")
  @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
  @Digits(integer = 10, fraction = 2, message = "El precio debe tener máximo 10 dígitos enteros y 2 decimales")
  @Schema(description = "Precio del producto", example = "1299.99", required = true)
  private BigDecimal price;

  @Size(max = 500, message = "La descripción no puede exceder los 500 caracteres")
  @Schema(description = "Descripción del producto", example = "Laptop ultrabook con procesador Intel i7")
  private String description;

  @Schema(description = "Estado activo del producto", example = "true", accessMode = Schema.AccessMode.READ_ONLY)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private Boolean active;

  @Schema(description = "Fecha de creación", example = "2025-07-23T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  private LocalDateTime createdAt;

  @Schema(description = "Fecha de última actualización", example = "2025-07-23T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  private LocalDateTime updatedAt;
}


