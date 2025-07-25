package com.example.products.service.impl;

import com.example.products.controller.dto.CreateProductDto;
import com.example.products.controller.dto.ProductDto;
import com.example.products.controller.dto.UpdateProductDto;
import com.example.products.exception.ProductAlreadyExistsException;
import com.example.products.exception.ProductNotFoundException;
import com.example.products.model.Product;
import com.example.products.repository.ProductRepository;
import com.example.products.service.KafkaProducerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

  @Mock
  private ProductRepository productRepository;

  @Mock
  private KafkaProducerService kafkaProducerService;

  @InjectMocks
  private ProductServiceImpl productService;

  @Captor
  private ArgumentCaptor<Product> productCaptor;

  private Product testProduct;
  private CreateProductDto createProductDto;
  private UpdateProductDto updateProductDto;

  @BeforeEach
  void setUp() {
    testProduct = Product.builder()
        .id(1L)
        .name("Test Product")
        .price(new BigDecimal("99.99"))
        .description("Test Description")
        .active(true)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();

    createProductDto = CreateProductDto.builder()
        .name("New Product")
        .price(new BigDecimal("149.99"))
        .description("New Description")
        .build();

    updateProductDto = UpdateProductDto.builder()
        .name("Updated Product")
        .price(new BigDecimal("199.99"))
        .description("Updated Description")
        .build();
  }

  @Test
  void createProduct_WhenProductDoesNotExist_ShouldCreateSuccessfully() {
    // Arrange
    when(productRepository.existsByNameIgnoreCase(createProductDto.getName())).thenReturn(false);
    when(productRepository.save(any(Product.class))).thenReturn(testProduct);

    // Act
    ProductDto result = productService.createProduct(createProductDto);

    // Assert
    assertNotNull(result);
    assertEquals(testProduct.getId(), result.getId());
    assertEquals(testProduct.getName(), result.getName());
    assertEquals(testProduct.getPrice(), result.getPrice());
    assertEquals(testProduct.getDescription(), result.getDescription());
    assertTrue(result.getActive());

    verify(productRepository).existsByNameIgnoreCase(createProductDto.getName());
    verify(productRepository).save(productCaptor.capture());
    verify(kafkaProducerService).publishProductCreatedEvent(testProduct);

    Product savedProduct = productCaptor.getValue();
    assertEquals(createProductDto.getName(), savedProduct.getName());
    assertEquals(createProductDto.getPrice(), savedProduct.getPrice());
    assertEquals(createProductDto.getDescription(), savedProduct.getDescription());
    assertTrue(savedProduct.getActive());
  }

  @Test
  void createProduct_WhenProductAlreadyExists_ShouldThrowException() {
    // Arrange
    when(productRepository.existsByNameIgnoreCase(createProductDto.getName())).thenReturn(true);

    // Act & Assert
    ProductAlreadyExistsException exception = assertThrows(
        ProductAlreadyExistsException.class,
        () -> productService.createProduct(createProductDto)
    );

    assertEquals("Ya existe un producto con el nombre: " + createProductDto.getName(),
        exception.getMessage());

    verify(productRepository).existsByNameIgnoreCase(createProductDto.getName());
    verify(productRepository, never()).save(any(Product.class));
    verify(kafkaProducerService, never()).publishProductCreatedEvent(any(Product.class));
  }

  @Test
  void getProductById_WhenProductExists_ShouldReturnProduct() {
    // Arrange
    when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(testProduct));

    // Act
    ProductDto result = productService.getProductById(1L);

    // Assert
    assertNotNull(result);
    assertEquals(testProduct.getId(), result.getId());
    assertEquals(testProduct.getName(), result.getName());
    verify(productRepository).findByIdAndActiveTrue(1L);
  }

  @Test
  void getProductById_WhenProductDoesNotExist_ShouldThrowException() {
    // Arrange
    when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.empty());

    // Act & Assert
    ProductNotFoundException exception = assertThrows(
        ProductNotFoundException.class,
        () -> productService.getProductById(1L)
    );

    assertEquals("Producto no encontrado con ID: 1", exception.getMessage());
    verify(productRepository).findByIdAndActiveTrue(1L);
  }

  @Test
  void getAllProducts_ShouldReturnPagedProducts() {
    // Arrange
    Pageable pageable = PageRequest.of(0, 10);
    List<Product> products = Arrays.asList(testProduct);
    Page<Product> productPage = new PageImpl<>(products, pageable, 1);

    when(productRepository.findByActiveTrue(pageable)).thenReturn(productPage);

    // Act
    Page<ProductDto> result = productService.getAllProducts(pageable);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.getTotalElements());
    assertEquals(1, result.getContent().size());

    ProductDto productDto = result.getContent().get(0);
    assertEquals(testProduct.getId(), productDto.getId());
    assertEquals(testProduct.getName(), productDto.getName());

    verify(productRepository).findByActiveTrue(pageable);
  }

  @Test
  void searchProductsByName_ShouldReturnMatchingProducts() {
    // Arrange
    String searchName = "Test";
    Pageable pageable = PageRequest.of(0, 10);
    List<Product> products = Arrays.asList(testProduct);
    Page<Product> productPage = new PageImpl<>(products, pageable, 1);

    when(productRepository.findByNameContainingIgnoreCaseAndActiveTrue(searchName, pageable))
        .thenReturn(productPage);

    // Act
    Page<ProductDto> result = productService.searchProductsByName(searchName, pageable);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.getTotalElements());
    verify(productRepository).findByNameContainingIgnoreCaseAndActiveTrue(searchName, pageable);
  }

  @Test
  void searchProductsByPriceRange_ShouldReturnProductsInRange() {
    // Arrange
    BigDecimal minPrice = new BigDecimal("50.00");
    BigDecimal maxPrice = new BigDecimal("150.00");
    Pageable pageable = PageRequest.of(0, 10);
    List<Product> products = Arrays.asList(testProduct);
    Page<Product> productPage = new PageImpl<>(products, pageable, 1);

    when(productRepository.findByPriceBetweenAndActiveTrue(minPrice, maxPrice, pageable))
        .thenReturn(productPage);

    // Act
    Page<ProductDto> result = productService.searchProductsByPriceRange(minPrice, maxPrice, pageable);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.getTotalElements());
    verify(productRepository).findByPriceBetweenAndActiveTrue(minPrice, maxPrice, pageable);
  }

  @Test
  void updateProduct_WhenProductExists_ShouldUpdateSuccessfully() {
    // Arrange
    when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(testProduct));
    when(productRepository.existsByNameIgnoreCaseAndIdNot(updateProductDto.getName(), 1L))
        .thenReturn(false);
    when(productRepository.save(any(Product.class))).thenReturn(testProduct);

    // Act
    ProductDto result = productService.updateProduct(1L, updateProductDto);

    // Assert
    assertNotNull(result);
    verify(productRepository).findByIdAndActiveTrue(1L);
    verify(productRepository).existsByNameIgnoreCaseAndIdNot(updateProductDto.getName(), 1L);
    verify(productRepository).save(productCaptor.capture());
    verify(kafkaProducerService).publishProductUpdatedEvent(any(Product.class));

    Product updatedProduct = productCaptor.getValue();
    assertEquals(updateProductDto.getName(), updatedProduct.getName());
    assertEquals(updateProductDto.getPrice(), updatedProduct.getPrice());
    assertEquals(updateProductDto.getDescription(), updatedProduct.getDescription());
  }

  @Test
  void updateProduct_WhenProductDoesNotExist_ShouldThrowException() {
    // Arrange
    when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.empty());

    // Act & Assert
    ProductNotFoundException exception = assertThrows(
        ProductNotFoundException.class,
        () -> productService.updateProduct(1L, updateProductDto)
    );

    assertEquals("Producto no encontrado con ID: 1", exception.getMessage());
    verify(productRepository).findByIdAndActiveTrue(1L);
    verify(productRepository, never()).save(any(Product.class));
    verify(kafkaProducerService, never()).publishProductUpdatedEvent(any(Product.class));
  }

  @Test
  void updateProduct_WhenNameAlreadyExists_ShouldThrowException() {
    // Arrange
    when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(testProduct));
    when(productRepository.existsByNameIgnoreCaseAndIdNot(updateProductDto.getName(), 1L))
        .thenReturn(true);

    // Act & Assert
    ProductAlreadyExistsException exception = assertThrows(
        ProductAlreadyExistsException.class,
        () -> productService.updateProduct(1L, updateProductDto)
    );

    assertEquals("Ya existe un producto con el nombre: " + updateProductDto.getName(),
        exception.getMessage());
    verify(productRepository, never()).save(any(Product.class));
    verify(kafkaProducerService, never()).publishProductUpdatedEvent(any(Product.class));
  }

  @Test
  void updateProduct_WithPartialUpdate_ShouldUpdateOnlyProvidedFields() {
    // Arrange
    UpdateProductDto partialUpdate = UpdateProductDto.builder()
        .name("New Name Only")
        .build(); // price y description son null

    when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(testProduct));
    when(productRepository.existsByNameIgnoreCaseAndIdNot(partialUpdate.getName(), 1L))
        .thenReturn(false);
    when(productRepository.save(any(Product.class))).thenReturn(testProduct);

    // Act
    productService.updateProduct(1L, partialUpdate);

    // Assert
    verify(productRepository).save(productCaptor.capture());

    Product updatedProduct = productCaptor.getValue();
    assertEquals(partialUpdate.getName(), updatedProduct.getName());
    // Price y description deberían mantener los valores originales
    assertEquals(testProduct.getPrice(), updatedProduct.getPrice());
    assertEquals(testProduct.getDescription(), updatedProduct.getDescription());
  }

  @Test
  void deactivateProduct_WhenProductExists_ShouldDeactivateSuccessfully() {
    // Arrange
    when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(testProduct));
    when(productRepository.save(any(Product.class))).thenReturn(testProduct);

    // Act
    productService.deactivateProduct(1L);

    // Assert
    verify(productRepository).findByIdAndActiveTrue(1L);
    verify(productRepository).save(productCaptor.capture());
    verify(kafkaProducerService).publishProductDeactivatedEvent(any(Product.class));

    Product deactivatedProduct = productCaptor.getValue();
    assertFalse(deactivatedProduct.getActive());
  }

  @Test
  void deactivateProduct_WhenProductDoesNotExist_ShouldThrowException() {
    // Arrange
    when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.empty());

    // Act & Assert
    ProductNotFoundException exception = assertThrows(
        ProductNotFoundException.class,
        () -> productService.deactivateProduct(1L)
    );

    assertEquals("Producto no encontrado con ID: 1", exception.getMessage());
    verify(productRepository, never()).save(any(Product.class));
    verify(kafkaProducerService, never()).publishProductDeactivatedEvent(any(Product.class));
  }

  @Test
  void activateProduct_WhenProductExists_ShouldActivateSuccessfully() {
    // Arrange
    testProduct.setActive(false); // Producto inactivo
    when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
    when(productRepository.save(any(Product.class))).thenReturn(testProduct);

    // Act
    ProductDto result = productService.activateProduct(1L);

    // Assert
    assertNotNull(result);
    verify(productRepository).findById(1L);
    verify(productRepository).save(productCaptor.capture());
    verify(kafkaProducerService).publishProductActivatedEvent(any(Product.class));

    Product activatedProduct = productCaptor.getValue();
    assertTrue(activatedProduct.getActive());
  }

  @Test
  void getTopExpensiveProducts_ShouldReturnLimitedProducts() {
    // Arrange
    List<Product> expensiveProducts = Arrays.asList(testProduct);
    when(productRepository.findTopExpensiveProducts(any(Pageable.class)))
        .thenReturn(expensiveProducts);

    // Act
    List<ProductDto> result = productService.getTopExpensiveProducts(5);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.size());
    verify(productRepository).findTopExpensiveProducts(any(Pageable.class));
  }

  @Test
  void getRecentProducts_ShouldReturnProductsFromLastDays() {
    // Arrange
    List<Product> recentProducts = Arrays.asList(testProduct);
    when(productRepository.findRecentProducts(any(LocalDate.class)))
        .thenReturn(recentProducts);

    // Act
    List<ProductDto> result = productService.getRecentProducts(7);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.size());
    verify(productRepository).findRecentProducts(any(LocalDate.class));
  }

  @Test
  void existsAndActive_WhenProductExists_ShouldReturnTrue() {
    // Arrange
    when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(testProduct));

    // Act
    boolean result = productService.existsAndActive(1L);

    // Assert
    assertTrue(result);
    verify(productRepository).findByIdAndActiveTrue(1L);
  }

  @Test
  void existsAndActive_WhenProductDoesNotExist_ShouldReturnFalse() {
    // Arrange
    when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.empty());

    // Act
    boolean result = productService.existsAndActive(1L);

    // Assert
    assertFalse(result);
    verify(productRepository).findByIdAndActiveTrue(1L);
  }

  @Test
  void countActiveProducts_ShouldReturnCount() {
    // Arrange
    when(productRepository.countByActiveTrue()).thenReturn(5L);

    // Act
    long result = productService.countActiveProducts();

    // Assert
    assertEquals(5L, result);
    verify(productRepository).countByActiveTrue();
  }

  @Test
  void searchProducts_WithAllCriteria_ShouldReturnMatchingProducts() {
    // Arrange
    String name = "Test";
    BigDecimal minPrice = new BigDecimal("50.00");
    BigDecimal maxPrice = new BigDecimal("150.00");
    Pageable pageable = PageRequest.of(0, 10);
    List<Product> products = Arrays.asList(testProduct);
    Page<Product> productPage = new PageImpl<>(products, pageable, 1);

    when(productRepository.findProductsByCriteria(name, minPrice, maxPrice, pageable))
        .thenReturn(productPage);

    // Act
    Page<ProductDto> result = productService.searchProducts(name, minPrice, maxPrice, pageable);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.getTotalElements());
    verify(productRepository).findProductsByCriteria(name, minPrice, maxPrice, pageable);
  }

  @Test
  void updateProduct_WhenSameNameAsExisting_ShouldUpdateSuccessfully() {
    // Arrange
    UpdateProductDto sameNameUpdate = UpdateProductDto.builder()
        .name(testProduct.getName()) // Mismo nombre
        .price(new BigDecimal("299.99"))
        .build();

    when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(testProduct));
    when(productRepository.save(any(Product.class))).thenReturn(testProduct);
    // No se llama existsByNameIgnoreCaseAndIdNot porque el nombre es el mismo

    // Act
    ProductDto result = productService.updateProduct(1L, sameNameUpdate);

    // Assert
    assertNotNull(result);
    verify(productRepository).findByIdAndActiveTrue(1L);
    verify(productRepository, never()).existsByNameIgnoreCaseAndIdNot(anyString(), anyLong());
    verify(productRepository).save(any(Product.class));
  }

  @Test
  void createProduct_WithNullFields_ShouldHandleGracefully() {
    // Arrange
    CreateProductDto dtoWithNulls = CreateProductDto.builder()
        .name("Product with nulls")
        .price(null) // Null price
        .description(null) // Null description
        .build();

    when(productRepository.existsByNameIgnoreCase(dtoWithNulls.getName())).thenReturn(false);
    when(productRepository.save(any(Product.class))).thenReturn(testProduct);

    // Act
    ProductDto result = productService.createProduct(dtoWithNulls);

    // Assert
    assertNotNull(result);
    verify(productRepository).save(productCaptor.capture());

    Product savedProduct = productCaptor.getValue();
    assertEquals(dtoWithNulls.getName(), savedProduct.getName());
    assertNull(savedProduct.getPrice());
    assertNull(savedProduct.getDescription());
    assertTrue(savedProduct.getActive());
  }
}