package com.example.products.config;

import com.example.products.service.KafkaProducerService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.mockito.Mockito;

@TestConfiguration
public class TestKafkaConfiguration {

  @Bean
  @Primary
  public KafkaProducerService mockKafkaProducerService() {
    return Mockito.mock(KafkaProducerService.class);
  }
}