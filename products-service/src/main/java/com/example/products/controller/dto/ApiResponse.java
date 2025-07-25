package com.example.products.controller.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO genérico para respuestas de la API
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Respuesta estándar de la API")
public class ApiResponse<T> {

  @Schema(description = "Indica si la operación fue exitosa", example = "true")
  private boolean success;

  @Schema(description = "Mensaje descriptivo del resultado", example = "Operación realizada exitosamente")
  private String message;

  @Schema(description = "Datos de respuesta")
  private T data;

  @Schema(description = "Lista de errores de validación")
  private List<ValidationError> errors;

  @Schema(description = "Timestamp de la respuesta", example = "2025-07-23T10:30:00")
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  @Builder.Default
  private LocalDateTime timestamp = LocalDateTime.now();

  @Schema(description = "Ruta del endpoint", example = "/api/v1/products")
  private String path;

  // Métodos de conveniencia para crear respuestas
  public static <T> ApiResponse<T> success(T data) {
    return ApiResponse.<T>builder()
        .success(true)
        .message("Operación realizada exitosamente")
        .data(data)
        .timestamp(LocalDateTime.now())
        .build();
  }

  public static <T> ApiResponse<T> success(T data, String message) {
    return ApiResponse.<T>builder()
        .success(true)
        .message(message)
        .data(data)
        .timestamp(LocalDateTime.now())
        .build();
  }

  public static <T> ApiResponse<T> error(String message) {
    return ApiResponse.<T>builder()
        .success(false)
        .message(message)
        .timestamp(LocalDateTime.now())
        .build();
  }

  public static <T> ApiResponse<T> error(String message, List<ValidationError> errors) {
    return ApiResponse.<T>builder()
        .success(false)
        .message(message)
        .errors(errors)
        .timestamp(LocalDateTime.now())
        .build();
  }

  public static <T> ApiResponse<T> error(String message, String path) {
    return ApiResponse.<T>builder()
        .success(false)
        .message(message)
        .path(path)
        .timestamp(LocalDateTime.now())
        .build();
  }
}
