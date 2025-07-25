package com.example.inventory.exception;

/**
 * Excepción lanzada cuando no se encuentra un producto
 */
public class ProductNotFoundException extends RuntimeException {

  public ProductNotFoundException(String message) {
    super(message);
  }

  public ProductNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}