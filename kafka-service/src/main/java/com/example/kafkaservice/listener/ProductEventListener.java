package com.example.kafkaservice.listener;

import com.example.kafkaservice.config.KafkaConfig;
import com.example.kafkaservice.model.KafkaEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductEventListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaConfig.PRODUCT_TOPIC, groupId = "${spring.kafka.consumer.group-id}")
    public void listen(ConsumerRecord<String, Object> record) {
        try {
            log.debug("Received Kafka event: {}", record);
            
            // Parse the event type from the value
            JsonNode jsonNode = objectMapper.valueToTree(record.value());
            String eventType = jsonNode.has("eventType") 
                    ? jsonNode.get("eventType").asText() 
                    : "UNKNOWN";
            
            // Create KafkaEvent for UI
            KafkaEvent event = KafkaEvent.builder()
                    .id(UUID.randomUUID().toString())
                    .topic(record.topic())
                    .partition(record.partition())
                    .offset(record.offset())
                    .key(record.key())
                    .value(objectMapper.writeValueAsString(record.value()))
                    .timestamp(LocalDateTime.now())
                    .type(parseEventType(eventType))
                    .build();
            
            // Send to WebSocket subscribers
            messagingTemplate.convertAndSend("/topic/events", event);
            
            log.debug("Sent event to WebSocket: {}", event);
        } catch (Exception e) {
            log.error("Error processing Kafka event", e);
        }
    }
    
    private KafkaEvent.EventType parseEventType(String eventType) {
        try {
            return KafkaEvent.EventType.valueOf(eventType);
        } catch (IllegalArgumentException e) {
            return KafkaEvent.EventType.CREATED; // Default to CREATED
        }
    }
}
