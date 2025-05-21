package com.example.productservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductEvent {
    private String eventType; // CREATED, UPDATED, DELETED
    private Long productId;
    private LocalDateTime timestamp = LocalDateTime.now();
    
    public ProductEvent(String eventType, Long productId) {
        this.eventType = eventType;
        this.productId = productId;
    }
}
