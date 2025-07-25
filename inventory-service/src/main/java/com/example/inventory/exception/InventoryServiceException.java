package com.example.inventory.exception;

/**
 * Excepción base para errores del servicio de inventario
 */
public class InventoryServiceException extends RuntimeException {

  public InventoryServiceException(String message) {
    super(message);
  }

  public InventoryServiceException(String message, Throwable cause) {
    super(message, cause);
  }
}