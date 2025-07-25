package com.example.products.service;

import com.example.products.controller.dto.CreateProductDto;
import com.example.products.controller.dto.ProductDto;
import com.example.products.controller.dto.UpdateProductDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

/**
 * Interfaz del servicio de productos
 */
public interface ProductService {

  /**
   * Crea un nuevo producto
   */
  ProductDto createProduct(CreateProductDto createProductDto);

  /**
   * Obtiene un producto por su ID
   */
  ProductDto getProductById(Long id);

  /**
   * Obtiene todos los productos activos con paginación
   */
  Page<ProductDto> getAllProducts(Pageable pageable);

  /**
   * Busca productos por nombre con paginación
   */
  Page<ProductDto> searchProductsByName(String name, Pageable pageable);

  /**
   * Busca productos por rango de precios
   */
  Page<ProductDto> searchProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

  /**
   * Busca productos por múltiples criterios
   */
  Page<ProductDto> searchProducts(String name, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

  /**
   * Actualiza un producto existente
   */
  ProductDto updateProduct(Long id, UpdateProductDto updateProductDto);

  /**
   * Desactiva un producto (soft delete)
   */
  void deactivateProduct(Long id);

  /**
   * Activa un producto
   */
  ProductDto activateProduct(Long id);

  /**
   * Obtiene los productos más caros
   */
  List<ProductDto> getTopExpensiveProducts(int limit);

  /**
   * Obtiene productos creados recientemente
   */
  List<ProductDto> getRecentProducts(int days);

  /**
   * Verifica si un producto existe y está activo
   */
  boolean existsAndActive(Long id);

  /**
   * Cuenta el total de productos activos
   */
  long countActiveProducts();
}