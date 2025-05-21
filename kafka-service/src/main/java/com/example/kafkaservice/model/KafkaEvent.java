package com.example.kafkaservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KafkaEvent {
    private String id;
    private String topic;
    private Integer partition;
    private Long offset;
    private String key;
    private String value;
    private LocalDateTime timestamp;
    private EventType type;
    
    public enum EventType {
        CREATED, UPDATED, DELETED
    }
}
