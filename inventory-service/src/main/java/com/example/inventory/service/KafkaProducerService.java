package com.example.inventory.service;

import com.example.inventory.model.Inventory;
import com.example.inventory.model.PurchaseHistory;

/**
 * Interfaz para el servicio de publicación de eventos Kafka del inventario
 */
public interface KafkaProducerService {

  /**
   * Publica evento de inventario actualizado
   *
   * @param inventory Inventario actualizado
   */
  void publishInventoryUpdatedEvent(Inventory inventory);

  /**
   * Publica evento de stock bajo
   *
   * @param inventory Inventario con stock bajo
   */
  void publishLowStockEvent(Inventory inventory);

  /**
   * Publica evento de producto fuera de stock
   *
   * @param inventory Inventario sin stock
   */
  void publishOutOfStockEvent(Inventory inventory);

  /**
   * Publica evento de compra realizada
   *
   * @param purchaseHistory Historial de compra
   * @param inventory Inventario actualizado
   */
  void publishPurchaseCompletedEvent(PurchaseHistory purchaseHistory, Inventory inventory);

  /**
   * Publica evento de compra cancelada
   *
   * @param purchaseHistory Historial de compra cancelada
   */
  void publishPurchaseCancelledEvent(PurchaseHistory purchaseHistory);

  /**
   * Publica evento de stock reservado
   *
   * @param productId ID del producto
   * @param quantity Cantidad reservada
   * @param referenceId ID de referencia
   */
  void publishStockReservedEvent(Long productId, Integer quantity, String referenceId);

  /**
   * Publica evento de liberación de stock reservado
   *
   * @param productId ID del producto
   * @param quantity Cantidad liberada
   * @param referenceId ID de referencia
   */
  void publishStockReleasedEvent(Long productId, Integer quantity, String referenceId);

  /**
   * Publica evento de ajuste de inventario
   *
   * @param inventory Inventario ajustado
   * @param previousQuantity Cantidad anterior
   * @param reason Razón del ajuste
   */
  void publishInventoryAdjustedEvent(Inventory inventory, Integer previousQuantity, String reason);

  /**
   * Publica evento de reposición de stock necesaria
   *
   * @param inventory Inventario que necesita reposición
   */
  void publishStockReplenishmentNeededEvent(Inventory inventory);
}