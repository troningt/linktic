package com.example.products.service.impl;

import com.example.products.controller.dto.CreateProductDto;
import com.example.products.controller.dto.ProductDto;
import com.example.products.controller.dto.UpdateProductDto;
import com.example.products.exception.ProductNotFoundException;
import com.example.products.exception.ProductAlreadyExistsException;
import com.example.products.model.Product;
import com.example.products.repository.ProductRepository;
import com.example.products.service.ProductService;
import com.example.products.service.KafkaProducerService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Implementación del servicio de productos
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

  private final ProductRepository productRepository;
  private final KafkaProducerService kafkaProducerService;

  @Override
  @Transactional
  public ProductDto createProduct(CreateProductDto createProductDto) {
    log.info("Creating new product with name: {}", createProductDto.getName());

    // Verificar si ya existe un producto con el mismo nombre
    if (productRepository.existsByNameIgnoreCase(createProductDto.getName())) {
      throw new ProductAlreadyExistsException("Ya existe un producto con el nombre: " + createProductDto.getName());
    }

    Product product = Product.builder()
        .name(createProductDto.getName())
        .price(createProductDto.getPrice())
        .description(createProductDto.getDescription())
        .active(true)
        .build();

    Product savedProduct = productRepository.save(product);

    // Publicar evento de creación de producto
    kafkaProducerService.publishProductCreatedEvent(savedProduct);

    log.info("Product created successfully with ID: {}", savedProduct.getId());
    return mapToDto(savedProduct);
  }

  @Override
  public ProductDto getProductById(Long id) {
    log.debug("Fetching product with ID: {}", id);

    Product product = productRepository.findByIdAndActiveTrue(id)
        .orElseThrow(() -> new ProductNotFoundException("Producto no encontrado con ID: " + id));

    return mapToDto(product);
  }

  @Override
  public Page<ProductDto> getAllProducts(Pageable pageable) {
    log.debug("Fetching all active products with pagination: page={}, size={}",
        pageable.getPageNumber(), pageable.getPageSize());

    Page<Product> products = productRepository.findByActiveTrue(pageable);
    return products.map(this::mapToDto);
  }

  @Override
  public Page<ProductDto> searchProductsByName(String name, Pageable pageable) {
    log.debug("Searching products by name: {} with pagination: page={}, size={}",
        name, pageable.getPageNumber(), pageable.getPageSize());

    Page<Product> products = productRepository.findByNameContainingIgnoreCaseAndActiveTrue(name, pageable);
    return products.map(this::mapToDto);
  }

  @Override
  public Page<ProductDto> searchProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
    log.debug("Searching products by price range: {} - {} with pagination: page={}, size={}",
        minPrice, maxPrice, pageable.getPageNumber(), pageable.getPageSize());

    Page<Product> products = productRepository.findByPriceBetweenAndActiveTrue(minPrice, maxPrice, pageable);
    return products.map(this::mapToDto);
  }

  @Override
  public Page<ProductDto> searchProducts(String name, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
    log.debug("Searching products by criteria - name: {}, minPrice: {}, maxPrice: {} with pagination: page={}, size={}",
        name, minPrice, maxPrice, pageable.getPageNumber(), pageable.getPageSize());

    Page<Product> products = productRepository.findProductsByCriteria(name, minPrice, maxPrice, pageable);
    return products.map(this::mapToDto);
  }

  @Override
  @Transactional
  public ProductDto updateProduct(Long id, UpdateProductDto updateProductDto) {
    log.info("Updating product with ID: {}", id);

    Product existingProduct = productRepository.findByIdAndActiveTrue(id)
        .orElseThrow(() -> new ProductNotFoundException("Producto no encontrado con ID: " + id));

    // Verificar si el nuevo nombre ya existe en otro producto
    if (updateProductDto.getName() != null &&
        !updateProductDto.getName().equalsIgnoreCase(existingProduct.getName()) &&
        productRepository.existsByNameIgnoreCaseAndIdNot(updateProductDto.getName(), id)) {
      throw new ProductAlreadyExistsException("Ya existe un producto con el nombre: " + updateProductDto.getName());
    }

    // Actualizar solo los campos que no son nulos
    if (updateProductDto.getName() != null) {
      existingProduct.setName(updateProductDto.getName());
    }
    if (updateProductDto.getPrice() != null) {
      existingProduct.setPrice(updateProductDto.getPrice());
    }
    if (updateProductDto.getDescription() != null) {
      existingProduct.setDescription(updateProductDto.getDescription());
    }

    Product updatedProduct = productRepository.save(existingProduct);

    // Publicar evento de actualización de producto
    kafkaProducerService.publishProductUpdatedEvent(updatedProduct);

    log.info("Product updated successfully with ID: {}", updatedProduct.getId());
    return mapToDto(updatedProduct);
  }

  @Override
  @Transactional
  public void deactivateProduct(Long id) {
    log.info("Deactivating product with ID: {}", id);

    Product product = productRepository.findByIdAndActiveTrue(id)
        .orElseThrow(() -> new ProductNotFoundException("Producto no encontrado con ID: " + id));

    product.setActive(false);
    productRepository.save(product);

    // Publicar evento de desactivación de producto
    kafkaProducerService.publishProductDeactivatedEvent(product);

    log.info("Product deactivated successfully with ID: {}", id);
  }

  @Override
  @Transactional
  public ProductDto activateProduct(Long id) {
    log.info("Activating product with ID: {}", id);

    Product product = productRepository.findById(id)
        .orElseThrow(() -> new ProductNotFoundException("Producto no encontrado con ID: " + id));

    product.setActive(true);
    Product activatedProduct = productRepository.save(product);

    // Publicar evento de activación de producto
    kafkaProducerService.publishProductActivatedEvent(activatedProduct);

    log.info("Product activated successfully with ID: {}", id);
    return mapToDto(activatedProduct);
  }

  @Override
  public List<ProductDto> getTopExpensiveProducts(int limit) {
    log.debug("Fetching top {} expensive products", limit);

    Pageable pageable = PageRequest.of(0, limit);
    List<Product> products = productRepository.findTopExpensiveProducts(pageable);

    return products.stream()
        .map(this::mapToDto)
        .toList();
  }

  @Override
  public List<ProductDto> getRecentProducts(int days) {
    log.debug("Fetching products created in the last {} days", days);
    LocalDate recentDate = LocalDate.now().minusDays(days);
    List<Product> products = productRepository.findRecentProducts(recentDate);

    return products.stream()
        .map(this::mapToDto)
        .toList();
  }

  @Override
  public boolean existsAndActive(Long id) {
    log.debug("Checking if product exists and is active with ID: {}", id);
    return productRepository.findByIdAndActiveTrue(id).isPresent();
  }

  @Override
  public long countActiveProducts() {
    log.debug("Counting active products");
    return productRepository.countByActiveTrue();
  }

  /**
   * Mapea una entidad Product a ProductDto
   */
  private ProductDto mapToDto(Product product) {
    return ProductDto.builder()
        .id(product.getId())
        .name(product.getName())
        .price(product.getPrice())
        .description(product.getDescription())
        .active(product.getActive())
        .createdAt(product.getCreatedAt())
        .updatedAt(product.getUpdatedAt())
        .build();
  }
}