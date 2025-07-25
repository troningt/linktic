package com.example.products.service.impl;

import com.example.products.event.ProductEvent;
import com.example.products.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaProducerServiceImplTest {

  @Mock
  private KafkaTemplate<String, Object> kafkaTemplate;

  @Mock
  private SendResult<String, Object> sendResult;

  @Mock
  private CompletableFuture<SendResult<String, Object>> future;

  @InjectMocks
  private KafkaProducerServiceImpl kafkaProducerService;

  @Captor
  private ArgumentCaptor<ProductEvent> eventCaptor;

  @Captor
  private ArgumentCaptor<String> keyCaptor;

  private Product testProduct;
  private final String TOPIC_NAME = "product-events-topic";

  @BeforeEach
  void setUp() {
    // Configurar el topic usando ReflectionTestUtils
    ReflectionTestUtils.setField(kafkaProducerService, "productEventsTopic", TOPIC_NAME);

    // Crear producto de prueba
    testProduct = Product.builder()
        .id(1L)
        .name("Test Product")
        .price(new BigDecimal("99.99"))
        .description("Test Description")
        .active(true)
        .build();
  }

  @Test
  void publishProductCreatedEvent_ShouldSendEventSuccessfully() {
    // Arrange
    when(kafkaTemplate.send(eq(TOPIC_NAME), eq("1"), any(ProductEvent.class)))
        .thenReturn(future);
    when(future.whenComplete(any())).thenReturn(future);

    // Act
    kafkaProducerService.publishProductCreatedEvent(testProduct);

    // Assert
    verify(kafkaTemplate).send(eq(TOPIC_NAME), keyCaptor.capture(), eventCaptor.capture());

    String capturedKey = keyCaptor.getValue();
    ProductEvent capturedEvent = eventCaptor.getValue();

    assertEquals("1", capturedKey);
    assertEquals("PRODUCT_CREATED", capturedEvent.getEventType());
    assertEquals(testProduct.getId(), capturedEvent.getProductId());
    assertEquals(testProduct.getName(), capturedEvent.getProductName());
    assertEquals(testProduct.getPrice(), capturedEvent.getPrice());
    assertEquals(testProduct.getDescription(), capturedEvent.getDescription());
    assertEquals(testProduct.getActive(), capturedEvent.getActive());
    assertNotNull(capturedEvent.getTimestamp());
  }

  @Test
  void publishProductUpdatedEvent_ShouldSendEventSuccessfully() {
    // Arrange
    when(kafkaTemplate.send(eq(TOPIC_NAME), eq("1"), any(ProductEvent.class)))
        .thenReturn(future);
    when(future.whenComplete(any())).thenReturn(future);

    // Act
    kafkaProducerService.publishProductUpdatedEvent(testProduct);

    // Assert
    verify(kafkaTemplate).send(eq(TOPIC_NAME), keyCaptor.capture(), eventCaptor.capture());

    ProductEvent capturedEvent = eventCaptor.getValue();
    assertEquals("PRODUCT_UPDATED", capturedEvent.getEventType());
    assertEquals(testProduct.getId(), capturedEvent.getProductId());
  }

  @Test
  void publishProductDeactivatedEvent_ShouldSendEventSuccessfully() {
    // Arrange
    testProduct.setActive(false);
    when(kafkaTemplate.send(eq(TOPIC_NAME), eq("1"), any(ProductEvent.class)))
        .thenReturn(future);
    when(future.whenComplete(any())).thenReturn(future);

    // Act
    kafkaProducerService.publishProductDeactivatedEvent(testProduct);

    // Assert
    verify(kafkaTemplate).send(eq(TOPIC_NAME), keyCaptor.capture(), eventCaptor.capture());

    ProductEvent capturedEvent = eventCaptor.getValue();
    assertEquals("PRODUCT_DEACTIVATED", capturedEvent.getEventType());
    assertEquals(testProduct.getId(), capturedEvent.getProductId());
    assertEquals(false, capturedEvent.getActive());
  }

  @Test
  void publishProductActivatedEvent_ShouldSendEventSuccessfully() {
    // Arrange
    when(kafkaTemplate.send(eq(TOPIC_NAME), eq("1"), any(ProductEvent.class)))
        .thenReturn(future);
    when(future.whenComplete(any())).thenReturn(future);

    // Act
    kafkaProducerService.publishProductActivatedEvent(testProduct);

    // Assert
    verify(kafkaTemplate).send(eq(TOPIC_NAME), keyCaptor.capture(), eventCaptor.capture());

    ProductEvent capturedEvent = eventCaptor.getValue();
    assertEquals("PRODUCT_ACTIVATED", capturedEvent.getEventType());
    assertEquals(testProduct.getId(), capturedEvent.getProductId());
    assertEquals(true, capturedEvent.getActive());
  }

  @Test
  void publishEvent_WhenKafkaTemplateFails_ShouldHandleException() {
    // Arrange
    when(kafkaTemplate.send(any(String.class), any(String.class), any(ProductEvent.class)))
        .thenThrow(new RuntimeException("Kafka connection failed"));

    // Act & Assert
    assertDoesNotThrow(() -> kafkaProducerService.publishProductCreatedEvent(testProduct));

    verify(kafkaTemplate).send(eq(TOPIC_NAME), eq("1"), any(ProductEvent.class));
  }

  @Test
  void publishEvent_WhenFutureCompletesWithException_ShouldLogError() {
    // Arrange
    CompletableFuture<SendResult<String, Object>> realFuture = new CompletableFuture<>();
    realFuture.completeExceptionally(new RuntimeException("Send failed"));

    when(kafkaTemplate.send(eq(TOPIC_NAME), eq("1"), any(ProductEvent.class)))
        .thenReturn(realFuture);

    // Act
    kafkaProducerService.publishProductCreatedEvent(testProduct);

    // Assert
    verify(kafkaTemplate).send(eq(TOPIC_NAME), eq("1"), any(ProductEvent.class));
    // El error se maneja en el callback whenComplete, no se propaga
  }

  @Test
  void publishEvent_WhenFutureCompletesSuccessfully_ShouldLogSuccess() {
    // Arrange
    CompletableFuture<SendResult<String, Object>> realFuture = new CompletableFuture<>();
    realFuture.complete(sendResult);

    when(kafkaTemplate.send(eq(TOPIC_NAME), eq("1"), any(ProductEvent.class)))
        .thenReturn(realFuture);

    // Act
    kafkaProducerService.publishProductCreatedEvent(testProduct);

    // Assert
    verify(kafkaTemplate).send(eq(TOPIC_NAME), eq("1"), any(ProductEvent.class));
  }

  @Test
  void publishProductCreatedEvent_WithNullProduct_ShouldHandleGracefully() {
    // Arrange
    Product nullProduct = null;

    // Act & Assert
    assertThrows(NullPointerException.class, () ->
        kafkaProducerService.publishProductCreatedEvent(nullProduct));
  }
}