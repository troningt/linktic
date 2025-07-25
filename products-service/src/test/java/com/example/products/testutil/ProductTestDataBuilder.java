package com.example.products.testutil;

import com.example.products.controller.dto.CreateProductDto;
import com.example.products.controller.dto.UpdateProductDto;
import com.example.products.model.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProductTestDataBuilder {

  public static Product.ProductBuilder defaultProduct() {
    return Product.builder()
        .name("Test Product")
        .price(new BigDecimal("99.99"))
        .description("Test Description")
        .active(true)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now());
  }

  public static CreateProductDto.CreateProductDtoBuilder defaultCreateProductDto() {
    return CreateProductDto.builder()
        .name("New Product")
        .price(new BigDecimal("149.99"))
        .description("New Description");
  }

  public static UpdateProductDto.UpdateProductDtoBuilder defaultUpdateProductDto() {
    return UpdateProductDto.builder()
        .name("Updated Product")
        .price(new BigDecimal("199.99"))
        .description("Updated Description");
  }

  public static Product inactiveProduct() {
    return defaultProduct()
        .name("Inactive Product")
        .active(false)
        .build();
  }

  public static Product expensiveProduct() {
    return defaultProduct()
        .name("Expensive Product")
        .price(new BigDecimal("999.99"))
        .build();
  }

  public static Product cheapProduct() {
    return defaultProduct()
        .name("Cheap Product")
        .price(new BigDecimal("9.99"))
        .build();
  }

  // Métodos de conveniencia para pruebas
  public static Product createProductWithName(String name) {
    return defaultProduct()
        .name(name)
        .build();
  }

  public static Product createProductWithPrice(BigDecimal price) {
    return defaultProduct()
        .price(price)
        .build();
  }

  public static Product createProductWithNameAndPrice(String name, BigDecimal price) {
    return defaultProduct()
        .name(name)
        .price(price)
        .build();
  }

  public static CreateProductDto createDtoWithName(String name) {
    return defaultCreateProductDto()
        .name(name)
        .build();
  }
}