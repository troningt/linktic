package com.example.inventory.service;

import com.example.inventory.controller.dto.ProductDto;

import java.util.Optional;

/**
 * Interfaz para el servicio de comunicación con el microservicio de productos
 */
public interface ProductService {

  /**
   * Obtiene la información de un producto por su ID
   *
   * @param productId ID del producto
   * @return ProductDto con la información del producto, Optional.empty() si no existe
   */
  Optional<ProductDto> getProductById(Long productId);

  /**
   * Verifica si un producto existe y está activo
   *
   * @param productId ID del producto
   * @return true si el producto existe y está activo
   */
  boolean isProductActiveById(Long productId);

  /**
   * Obtiene la información básica de un producto con manejo de errores
   *
   * @param productId ID del producto
   * @return ProductDto con información básica, null si hay error
   */
  ProductDto getProductInfoSafely(Long productId);

  /**
   * Verifica la conectividad con el servicio de productos
   *
   * @return true si el servicio está disponible
   */
  boolean isProductServiceAvailable();

  /**
   * Obtiene información de producto con reintentos automáticos
   *
   * @param productId ID del producto
   * @param maxRetries Número máximo de reintentos
   * @return ProductDto del producto
   */
  Optional<ProductDto> getProductWithRetry(Long productId, int maxRetries);
}