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
        for (Long id : ids) {           // MCP VIBE-003 (reported on the next line): N+1, one DB call per iteration
            CompletableFuture<Order> future = repository.findByIdAsync(id);
            try {
                Order order = future.get();  // CLI blocking: Future.get() in @Async (not detected by the MCP)
            } catch (Exception ignored) {}
        }
    }
}
