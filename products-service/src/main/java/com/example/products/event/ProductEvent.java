package com.example.products.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Evento de producto para Kafka
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductEvent {

  private String eventType;
  private Long productId;
  private String productName;
  private BigDecimal price;
  private String description;
  private Boolean active;

  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  private LocalDateTime timestamp;
}