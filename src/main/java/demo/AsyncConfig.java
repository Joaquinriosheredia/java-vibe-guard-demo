package demo;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

// Without @EnableAsync, Spring ignores @Async and runs the method on the caller's thread,
// so OrderService's @Async would have no effect (and java-vibe-guard >= 2.3.0 does not
// report it).
@Configuration
@EnableAsync
public class AsyncConfig {
}
