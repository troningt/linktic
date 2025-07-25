package com.example.products.controller.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para respuestas paginadas con datos
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Respuesta paginada de la API")
public class PagedApiResponse<T> {

  @Schema(description = "Indica si la operación fue exitosa", example = "true")
  private boolean success;

  @Schema(description = "Mensaje descriptivo del resultado", example = "Datos obtenidos exitosamente")
  private String message;

  @Schema(description = "Lista de datos")
  private List<T> data;

  @Schema(description = "Información de paginación")
  private PageInfo pageInfo;

  @Schema(description = "Timestamp de la respuesta", example = "2025-07-23T10:30:00")
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  @Builder.Default
  private LocalDateTime timestamp = LocalDateTime.now();

  public static <T> PagedApiResponse<T> success(List<T> data, PageInfo pageInfo) {
    return PagedApiResponse.<T>builder()
        .success(true)
        .message("Datos obtenidos exitosamente")
        .data(data)
        .pageInfo(pageInfo)
        .timestamp(LocalDateTime.now())
        .build();
  }

  public static <T> PagedApiResponse<T> success(List<T> data, PageInfo pageInfo, String message) {
    return PagedApiResponse.<T>builder()
        .success(true)
        .message(message)
        .data(data)
        .pageInfo(pageInfo)
        .timestamp(LocalDateTime.now())
        .build();
  }
}
