package com.example.inventory.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuración de Apache Kafka
 */
@Configuration
@EnableKafka
public class KafkaConfig {

  @Value("${spring.kafka.bootstrap-servers}")
  private String bootstrapServers;

  @Value("${spring.kafka.consumer.group-id}")
  private String groupId;

  @Value("${app.kafka.topics.inventory-events}")
  private String inventoryEventsTopic;

  @Value("${app.kafka.topics.purchase-events}")
  private String purchaseEventsTopic;

  @Value("${app.kafka.topics.stock-alerts}")
  private String stockAlertsTopic;

  // ==================== PRODUCER CONFIGURATION ====================

  @Bean
  public ProducerFactory<String, Object> producerFactory() {
    Map<String, Object> configProps = new HashMap<>();
    configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    configProps.put(ProducerConfig.ACKS_CONFIG, "all");
    configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
    configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
    configProps.put(ProducerConfig.LINGER_MS_CONFIG, 5);
    configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
    configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
    configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
    configProps.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000);

    return new DefaultKafkaProducerFactory<>(configProps);
  }

  @Bean
  public KafkaTemplate<String, Object> kafkaTemplate() {
    return new KafkaTemplate<>(producerFactory());
  }

  // ==================== CONSUMER CONFIGURATION ====================

  @Bean
  public ConsumerFactory<String, Object> consumerFactory() {
    Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
    props.put(JsonDeserializer.TRUSTED_PACKAGES,
        "com.example.inventory.event,com.example.products.event");
    props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
    props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000);
    props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);
    props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);

    return new DefaultKafkaConsumerFactory<>(props);
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
    ConcurrentKafkaListenerContainerFactory<String, Object> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory());
    factory.setConcurrency(3);
    factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
    factory.getContainerProperties().setPollTimeout(3000);
    return factory;
  }

  // ==================== TOPIC CONFIGURATION ====================

  @Bean
  public NewTopic inventoryEventsTopic() {
    return TopicBuilder.name(inventoryEventsTopic)
        .partitions(3)
        .replicas(1)
        .config("retention.ms", "604800000") // 7 días
        .config("compression.type", "lz4")
        .build();
  }

  @Bean
  public NewTopic purchaseEventsTopic() {
    return TopicBuilder.name(purchaseEventsTopic)
        .partitions(3)
        .replicas(1)
        .config("retention.ms", "2592000000") // 30 días
        .config("compression.type", "lz4")
        .build();
  }

  @Bean
  public NewTopic stockAlertsTopic() {
    return TopicBuilder.name(stockAlertsTopic)
        .partitions(2)
        .replicas(1)
        .config("retention.ms", "86400000") // 1 día
        .config("compression.type", "lz4")
        .build();
  }
}