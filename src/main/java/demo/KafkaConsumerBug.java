package demo;

import org.springframework.kafka.annotation.KafkaListener;

public class KafkaConsumerBug {

    @KafkaListener(topics = "orders")   // CLI kafka (warning) · MCP VIBE-006 (warning): missing groupId
    public void consume(String message) throws InterruptedException {
        Thread.sleep(10);               // CLI blocking-kafka · MCP VIBE-006: blocking call in a listener
        System.out.println(message);
    }
}
