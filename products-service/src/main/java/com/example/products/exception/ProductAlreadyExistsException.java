package com.example.products.exception;

/**
 * Excepción lanzada cuando se intenta crear un producto que ya existe
 */
public class ProductAlreadyExistsException extends RuntimeException {
  public ProductAlreadyExistsException(String message) {
    super(message);
  }

  public ProductAlreadyExistsException(String message, Throwable cause) {
    super(message, cause);
  }
}
