package demo;

import org.springframework.kafka.annotation.KafkaListener;

public class KafkaConsumerBug {

    @KafkaListener(topics = "orders")   // VIBE-006: missing groupId
    public void consume(String message) throws InterruptedException {
        Thread.sleep(10);               // VIBE-002: Thread.sleep() inside KafkaListener method
        System.out.println(message);
    }
}
