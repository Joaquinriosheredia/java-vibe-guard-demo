package demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.scheduling.annotation.Async;
import reactor.core.publisher.Mono;

@RestController
public class ReactiveController {

    @GetMapping("/data")
    @Async                               // async boundary — must NOT block
    public Mono<String> getData() {
        String result = Mono.just("hello").block();  // VIBE-002: blocking .block() in @Async method
        return Mono.just(result);
    }
}
