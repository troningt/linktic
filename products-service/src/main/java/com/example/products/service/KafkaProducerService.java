package com.example.products.service;

import com.example.products.model.Product;
/**
 * Interfaz para el servicio de publicación de eventos Kafka
 */
public interface KafkaProducerService {

  /**
   * Publica evento de producto creado
   */
  void publishProductCreatedEvent(Product product);

  /**
   * Publica evento de producto actualizado
   */
  void publishProductUpdatedEvent(Product product);

  /**
   * Publica evento de producto desactivado
   */
  void publishProductDeactivatedEvent(Product product);

  /**
   * Publica evento de producto activado
   */
  void publishProductActivatedEvent(Product product);
}
