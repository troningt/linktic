package com.example.products.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para errores de validación
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Error de validación")
public class ValidationError {

  @Schema(description = "Campo que tiene el error", example = "name")
  private String field;

  @Schema(description = "Valor que causó el error", example = "")
  private Object rejectedValue;

  @Schema(description = "Mensaje de error", example = "El nombre del producto es obligatorio")
  private String message;

  public ValidationError(String field, String message) {
    this.field = field;
    this.message = message;
  }
}
