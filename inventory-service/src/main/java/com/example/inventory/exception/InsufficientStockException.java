package com.example.inventory.exception;

/**
 * Excepción lanzada cuando no hay suficiente stock disponible
 */
public class InsufficientStockException extends RuntimeException {

  public InsufficientStockException(String message) {
    super(message);
  }

  public InsufficientStockException(String message, Throwable cause) {
    super(message, cause);
  }
}