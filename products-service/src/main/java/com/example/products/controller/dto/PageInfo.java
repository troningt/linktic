package com.example.products.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para respuestas paginadas
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Información de paginación")
public class PageInfo {

  @Schema(description = "Número de página actual", example = "0")
  private int page;

  @Schema(description = "Tamaño de página", example = "10")
  private int size;

  @Schema(description = "Número total de elementos", example = "100")
  private long totalElements;

  @Schema(description = "Número total de páginas", example = "10")
  private int totalPages;

  @Schema(description = "Indica si es la primera página", example = "true")
  private boolean first;

  @Schema(description = "Indica si es la última página", example = "false")
  private boolean last;

  @Schema(description = "Indica si hay página anterior", example = "false")
  private boolean hasPrevious;

  @Schema(description = "Indica si hay página siguiente", example = "true")
  private boolean hasNext;
}