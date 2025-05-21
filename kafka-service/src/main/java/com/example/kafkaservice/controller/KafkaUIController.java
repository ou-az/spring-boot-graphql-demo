package com.example.kafkaservice.controller;

import com.example.kafkaservice.model.KafkaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.TopicListing;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Controller
@RequestMapping("/kafka-ui")
@RequiredArgsConstructor
@Slf4j
public class KafkaUIController {

    private final KafkaAdmin kafkaAdmin;
    
    @GetMapping
    public String dashboard(Model model) {
        try {
            List<String> topics = getKafkaTopics();
            model.addAttribute("topics", topics);
            return "kafka-dashboard";
        } catch (Exception e) {
            log.error("Error loading Kafka dashboard", e);
            model.addAttribute("error", "Failed to load Kafka topics: " + e.getMessage());
            return "error";
        }
    }
    
    @GetMapping("/topics")
    @ResponseBody
    public List<String> getTopics() {
        try {
            return getKafkaTopics();
        } catch (Exception e) {
            log.error("Error getting Kafka topics", e);
            return new ArrayList<>();
        }
    }
    
    private List<String> getKafkaTopics() throws ExecutionException, InterruptedException {
        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            ListTopicsResult topics = adminClient.listTopics();
            Collection<TopicListing> topicListings = topics.listings().get();
            
            List<String> topicNames = new ArrayList<>();
            for (TopicListing topicListing : topicListings) {
                if (!topicListing.name().startsWith("_")) { // Filter out internal topics
                    topicNames.add(topicListing.name());
                }
            }
            
            return topicNames;
        }
    }
}
