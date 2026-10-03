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
        String result = Mono.just("hello").block();  // CLI reactor-block · MCP VIBE-002 — fired by the Spring bean + Reactor import, not by @Async
        return Mono.just(result);
    }
}
