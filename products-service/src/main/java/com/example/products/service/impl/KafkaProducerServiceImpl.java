package com.example.products.service.impl;

import com.example.products.event.ProductEvent;
import com.example.products.model.Product;
import com.example.products.service.KafkaProducerService;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
/**
 * Implementación del servicio de publicación de eventos Kafka
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerServiceImpl implements KafkaProducerService {

  private final KafkaTemplate<String, Object> kafkaTemplate;

  @Value("${app.kafka.topics.product-events}")
  private String productEventsTopic;

  @Override
  public void publishProductCreatedEvent(Product product) {
    ProductEvent event = ProductEvent.builder()
        .eventType("PRODUCT_CREATED")
        .productId(product.getId())
        .productName(product.getName())
        .price(product.getPrice())
        .description(product.getDescription())
        .active(product.getActive())
        .timestamp(LocalDateTime.now())
        .build();

    publishEvent(event, "Product created event published for product ID: " + product.getId());
  }

  @Override
  public void publishProductUpdatedEvent(Product product) {
    ProductEvent event = ProductEvent.builder()
        .eventType("PRODUCT_UPDATED")
        .productId(product.getId())
        .productName(product.getName())
        .price(product.getPrice())
        .description(product.getDescription())
        .active(product.getActive())
        .timestamp(LocalDateTime.now())
        .build();

    publishEvent(event, "Product updated event published for product ID: " + product.getId());
  }

  @Override
  public void publishProductDeactivatedEvent(Product product) {
    ProductEvent event = ProductEvent.builder()
        .eventType("PRODUCT_DEACTIVATED")
        .productId(product.getId())
        .productName(product.getName())
        .price(product.getPrice())
        .description(product.getDescription())
        .active(product.getActive())
        .timestamp(LocalDateTime.now())
        .build();

    publishEvent(event, "Product deactivated event published for product ID: " + product.getId());
  }

  @Override
  public void publishProductActivatedEvent(Product product) {
    ProductEvent event = ProductEvent.builder()
        .eventType("PRODUCT_ACTIVATED")
        .productId(product.getId())
        .productName(product.getName())
        .price(product.getPrice())
        .description(product.getDescription())
        .active(product.getActive())
        .timestamp(LocalDateTime.now())
        .build();

    publishEvent(event, "Product activated event published for product ID: " + product.getId());
  }

  /**
   * Método privado para publicar eventos en Kafka
   */
  private void publishEvent(ProductEvent event, String logMessage) {
    try {
      CompletableFuture<SendResult<String, Object>> future =
          kafkaTemplate.send(productEventsTopic, event.getProductId().toString(), event);

      future.whenComplete((result, ex) -> {
        if (ex == null) {
          log.info("{} - Offset: {}, Partition: {}",
              logMessage,
              result.getRecordMetadata().offset(),
              result.getRecordMetadata().partition());
        } else {
          log.error("Failed to publish event for product ID: {} - Error: {}",
              event.getProductId(), ex.getMessage(), ex);
        }
      });
    } catch (Exception e) {
      log.error("Error publishing event for product ID: {} - Error: {}",
          event.getProductId(), e.getMessage(), e);
    }
  }
}
