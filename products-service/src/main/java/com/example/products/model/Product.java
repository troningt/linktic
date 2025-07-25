package com.example.products.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad Product que representa un producto en el sistema
 */
@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_product_name", columnList = "name")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"createdAt", "updatedAt"})
public class Product {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @NotBlank(message = "El nombre del producto es obligatorio")
  @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @NotNull(message = "El precio es obligatorio")
  @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
  @Digits(integer = 10, fraction = 2, message = "El precio debe tener máximo 10 dígitos enteros y 2 decimales")
  @Column(name = "price", nullable = false, precision = 12, scale = 2)
  private BigDecimal price;

  @Size(max = 500, message = "La descripción no puede exceder los 500 caracteres")
  @Column(name = "description", length = 500)
  private String description;

  @Builder.Default
  @Column(name = "active", nullable = false)
  private Boolean active = true;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Version
  @Column(name = "version")
  private Integer version;
}