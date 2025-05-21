package com.example.productservice.config;

import com.example.productservice.event.ProductEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;

import java.time.Duration;

@Configuration
public class KafkaConfig {

    public static final String PRODUCT_TOPIC = "product-events";
    public static final String PRODUCT_DLT_TOPIC = "product-events.DLT";

    @Bean
    public NewTopic productTopic() {
        return TopicBuilder.name(PRODUCT_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic productDltTopic() {
        return TopicBuilder.name(PRODUCT_DLT_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public RecordMessageConverter converter() {
        return new StringJsonMessageConverter();
    }

    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<String, ProductEvent> template) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template);
        return new DefaultErrorHandler(recoverer, Duration.ofSeconds(1), Duration.ofSeconds(2), 
                Duration.ofSeconds(3), Duration.ofSeconds(5), Duration.ofSeconds(10));
    }
}
