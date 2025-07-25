package com.example.products.service.impl;

import com.example.products.controller.dto.CreateProductDto;
import com.example.products.controller.dto.ProductDto;
import com.example.products.controller.dto.UpdateProductDto;
import com.example.products.exception.ProductAlreadyExistsException;
import com.example.products.exception.ProductNotFoundException;
import com.example.products.model.Product;
import com.example.products.repository.ProductRepository;
import com.example.products.service.KafkaProducerService;
import com.example.products.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductServiceSpringBootTest {

  @Autowired
  private ProductRepository productRepository;

  @MockBean
  private KafkaProducerService kafkaProducerService;

  @Autowired
  private ProductService productService;

  @BeforeEach
  void setUp() {
    productRepository.deleteAll();
  }

  @Test
  void createProduct_FullIntegrationTest_ShouldCreateAndPersist() {
    // Arrange
    CreateProductDto createDto = CreateProductDto.builder()
        .name("Integration Test Product")
        .price(new BigDecimal("199.99"))
        .description("Integration test description")
        .build();

    // Act
    ProductDto result = productService.createProduct(createDto);

    // Assert
    assertNotNull(result);
    assertNotNull(result.getId());
    assertEquals(createDto.getName(), result.getName());
    assertEquals(createDto.getPrice(), result.getPrice());
    assertEquals(createDto.getDescription(), result.getDescription());
    assertTrue(result.getActive());
    assertNotNull(result.getCreatedAt());
    assertNotNull(result.getUpdatedAt());

    // Verificar que se guardó en la base de datos
    assertTrue(productRepository.existsById(result.getId()));
    Product savedProduct = productRepository.findById(result.getId()).orElseThrow();
    assertEquals(createDto.getName(), savedProduct.getName());
  }

  @Test
  void createProduct_WithDuplicateName_ShouldThrowException() {
    // Arrange
    Product existingProduct = Product.builder()
        .name("Duplicate Name")
        .price(new BigDecimal("50.00"))
        .description("Existing product")
        .active(true)
        .build();
    productRepository.save(existingProduct);

    CreateProductDto duplicateDto = CreateProductDto.builder()
        .name("Duplicate Name")
        .price(new BigDecimal("75.00"))
        .description("Duplicate product")
        .build();

    // Act & Assert
    ProductAlreadyExistsException exception = assertThrows(
        ProductAlreadyExistsException.class,
        () -> productService.createProduct(duplicateDto)
    );

    assertTrue(exception.getMessage().contains("Duplicate Name"));
  }

  @Test
  void updateProduct_FullIntegrationTest_ShouldUpdateCorrectly() {
    // Arrange
    Product originalProduct = Product.builder()
        .name("Original Product")
        .price(new BigDecimal("100.00"))
        .description("Original description")
        .active(true)
        .build();
    Product savedProduct = productRepository.save(originalProduct);

    UpdateProductDto updateDto = UpdateProductDto.builder()
        .name("Updated Product")
        .price(new BigDecimal("150.00"))
        .description("Updated description")
        .build();

    // Act
    ProductDto result = productService.updateProduct(savedProduct.getId(), updateDto);

    // Assert
    assertEquals(savedProduct.getId(), result.getId());
    assertEquals(updateDto.getName(), result.getName());
    assertEquals(updateDto.getPrice(), result.getPrice());
    assertEquals(updateDto.getDescription(), result.getDescription());

    // Verificar en base de datos
    Product updatedProduct = productRepository.findById(savedProduct.getId()).orElseThrow();
    assertEquals(updateDto.getName(), updatedProduct.getName());
    assertEquals(updateDto.getPrice(), updatedProduct.getPrice());
    assertEquals(updateDto.getDescription(), updatedProduct.getDescription());
  }

  @Test
  void getAllProducts_WithPagination_ShouldReturnCorrectPage() {
    // Arrange
    for (int i = 1; i <= 15; i++) {
      Product product = Product.builder()
          .name("Product " + i)
          .price(new BigDecimal("10.00").multiply(BigDecimal.valueOf(i)))
          .description("Description " + i)
          .active(true)
          .build();
      productRepository.save(product);
    }

    Pageable firstPage = PageRequest.of(0, 10);
    Pageable secondPage = PageRequest.of(1, 10);

    // Act
    Page<ProductDto> firstPageResult = productService.getAllProducts(firstPage);
    Page<ProductDto> secondPageResult = productService.getAllProducts(secondPage);

    // Assert
    assertEquals(15, firstPageResult.getTotalElements());
    assertEquals(2, firstPageResult.getTotalPages());
    assertEquals(10, firstPageResult.getContent().size());
    assertEquals(5, secondPageResult.getContent().size());
  }

  @Test
  void searchProductsByName_CaseInsensitive_ShouldWork() {
    // Arrange
    Product product1 = Product.builder()
        .name("Gaming Laptop")
        .price(new BigDecimal("1000.00"))
        .description("High-end gaming laptop")
        .active(true)
        .build();

    Product product2 = Product.builder()
        .name("Office Laptop")
        .price(new BigDecimal("500.00"))
        .description("Business laptop")
        .active(true)
        .build();

    productRepository.save(product1);
    productRepository.save(product2);

    Pageable pageable = PageRequest.of(0, 10);

    // Act
    Page<ProductDto> gamingResults = productService.searchProductsByName("GAMING", pageable);
    Page<ProductDto> laptopResults = productService.searchProductsByName("laptop", pageable);

    // Assert
    assertEquals(1, gamingResults.getTotalElements());
    assertEquals("Gaming Laptop", gamingResults.getContent().get(0).getName());

    assertEquals(2, laptopResults.getTotalElements());
  }

  @Test
  void deactivateAndActivateProduct_ShouldWorkCorrectly() {
    // Arrange
    Product product = Product.builder()
        .name("Test Product")
        .price(new BigDecimal("50.00"))
        .description("Test description")
        .active(true)
        .build();
    Product savedProduct = productRepository.save(product);

    // Act - Deactivate
    productService.deactivateProduct(savedProduct.getId());

    // Assert - Should be deactivated
    Product deactivatedProduct = productRepository.findById(savedProduct.getId()).orElseThrow();
    assertFalse(deactivatedProduct.getActive());

    // Should not be found in active searches
    assertThrows(ProductNotFoundException.class,
        () -> productService.getProductById(savedProduct.getId()));

    // Act - Activate
    ProductDto reactivatedDto = productService.activateProduct(savedProduct.getId());

    // Assert - Should be active again
    assertTrue(reactivatedDto.getActive());
    Product reactivatedProduct = productRepository.findById(savedProduct.getId()).orElseThrow();
    assertTrue(reactivatedProduct.getActive());

    // Should be found in active searches again
    assertDoesNotThrow(() -> productService.getProductById(savedProduct.getId()));
  }

  @Test
  void searchProductsByPriceRange_ShouldFilterCorrectly() {
    // Arrange
    Product cheap = Product.builder()
        .name("Cheap Product")
        .price(new BigDecimal("25.00"))
        .description("Affordable option")
        .active(true)
        .build();

    Product medium = Product.builder()
        .name("Medium Product")
        .price(new BigDecimal("75.00"))
        .description("Mid-range option")
        .active(true)
        .build();

    Product expensive = Product.builder()
        .name("Expensive Product")
        .price(new BigDecimal("200.00"))
        .description("Premium option")
        .active(true)
        .build();

    productRepository.save(cheap);
    productRepository.save(medium);
    productRepository.save(expensive);

    Pageable pageable = PageRequest.of(0, 10);

    // Act
    Page<ProductDto> results = productService.searchProductsByPriceRange(
        new BigDecimal("50.00"),
        new BigDecimal("100.00"),
        pageable
    );

    // Assert
    assertEquals(1, results.getTotalElements());
    assertEquals("Medium Product", results.getContent().get(0).getName());
    assertEquals(new BigDecimal("75.00"), results.getContent().get(0).getPrice());
  }

  @Test
  void countActiveProducts_ShouldReturnCorrectCount() {
    // Arrange
    for (int i = 1; i <= 5; i++) {
      Product activeProduct = Product.builder()
          .name("Active Product " + i)
          .price(new BigDecimal("10.00"))
          .description("Active product")
          .active(true)
          .build();
      productRepository.save(activeProduct);
    }

    for (int i = 1; i <= 3; i++) {
      Product inactiveProduct = Product.builder()
          .name("Inactive Product " + i)
          .price(new BigDecimal("10.00"))
          .description("Inactive product")
          .active(false)
          .build();
      productRepository.save(inactiveProduct);
    }

    // Act
    long activeCount = productService.countActiveProducts();

    // Assert
    assertEquals(5, activeCount);
  }
}