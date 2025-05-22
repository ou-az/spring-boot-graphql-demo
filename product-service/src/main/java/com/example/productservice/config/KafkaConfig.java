package com.example.productservice.config;

import com.example.productservice.event.ProductEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;

import java.time.Duration;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = true)
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
        // Use a compatible constructor with ExponentialBackOff
        ExponentialBackOff backOff = new ExponentialBackOff(
            1000L, // initial interval
            2.0    // multiplier
        );
        
        // Set additional properties
        backOff.setMaxInterval(10000L); // max interval
        return new DefaultErrorHandler(recoverer, backOff);
    }
}
