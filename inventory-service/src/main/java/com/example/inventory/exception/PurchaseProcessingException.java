package com.example.inventory.exception;

/**
 * Excepción lanzada cuando hay errores procesando una compra
 */
public class PurchaseProcessingException extends RuntimeException {

  public PurchaseProcessingException(String message) {
    super(message);
  }

  public PurchaseProcessingException(String message, Throwable cause) {
    super(message, cause);
  }
}