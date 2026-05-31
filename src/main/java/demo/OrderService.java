package demo;

import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class OrderService {

    private final OrderRepository repository;

    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }

    @Async                              // async boundary — must NOT block
    public void processOrders(List<Long> ids) {
        for (Long id : ids) {           // VIBE-003: N+1 — one DB call per iteration
            CompletableFuture<Order> future = repository.findByIdAsync(id);
            try {
                Order order = future.get();  // VIBE-002: blocking .get() in @Async
            } catch (Exception ignored) {}
        }
    }
}
