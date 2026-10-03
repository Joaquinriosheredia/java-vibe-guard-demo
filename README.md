# java-vibe-guard-demo

Minimal reproducible demo for [java-vibe-guard](https://github.com/Joaquinriosheredia/java-vibe-guard).

This project contains exactly **3 plain `.java` files** that produce **3 CRITICAL findings** when scanned with the CLI — no Maven, no Gradle, no runtime dependencies.

## Run

```bash
npx java-vibe-guard@2.0.0 .
```

Exit code `1` (CRITICAL findings present).

## Expected output (java-vibe-guard 2.0.0)

Real output, captured with `npx java-vibe-guard@2.0.0 . --no-color`:

```
java-vibe-guard — vibe coding detector for Java/Spring Boot
Scanning: .  (3 files)

❌ CRITICAL: Thread.sleep() detected in @KafkaListener method → src/main/java/demo/KafkaConsumerBug.java:9
  Evidence: documented mechanism, no benchmark of our own — a blocking call delays the consumer's next poll(); past max.poll.interval.ms the group coordinator considers the consumer dead and rebalances the group (Kafka consumer docs)
❌ CRITICAL: blocking Future.get() detected in @Async method → src/main/java/demo/OrderService.java:22
  Evidence: documented mechanism, no benchmark of our own — the call holds a thread of the @Async executor / @Scheduled scheduler / event pool for its whole duration; under load the pool saturates
❌ CRITICAL: Reactive blocking call '.block()' inside Spring bean — pins a thread under load; use reactive composition (.flatMap, .map, .then) instead → src/main/java/demo/ReactiveController.java:14
  Evidence: documented mechanism, no benchmark of our own — blocking pins a Reactor thread (Netty event loop or Schedulers.parallel() worker) for the whole I/O wait; with few such threads, throughput collapses under load
⚠️  WARNING: @KafkaListener without explicit groupId → src/main/java/demo/KafkaConsumerBug.java:7
⚠️  WARNING: @KafkaListener without @RetryableTopic or DLQ — failed messages will be lost → src/main/java/demo/KafkaConsumerBug.java:7
⚠️  WARNING: Endpoint without structured logging → src/main/java/demo/ReactiveController.java:11

──────────────────────────────────────────────────────────────
📊 Summary: 3 critical · 3 warnings
──────────────────────────────────────────────────────────────

🚨 3 CRITICAL issue(s) found — fix before deploying to production.
```

The `Evidence:` lines show what each rule rests on. None of these three rules has a benchmark of its own yet, and the output says so instead of borrowing a figure from a benchmark that measured something else.

## Files and bugs

java-vibe-guard ships two independent Java engines with different rule ids: the **CLI** (npm package / GitHub Action) and the **MCP server** (Claude Code). Both were run on this project; this is what each one reports:

| File | Bug | CLI (`npx java-vibe-guard`) | MCP server (`analyzeProject`) |
|------|-----|-----------------------------|-------------------------------|
| `ReactiveController.java` | `Mono.just(...).block()` in a `@RestController` | `reactor-block` CRITICAL :14 | `VIBE-002` CRITICAL :14 |
| `ReactiveController.java` | endpoint without structured logging | `observability` WARNING :11 | — |
| `KafkaConsumerBug.java` | `Thread.sleep()` inside `@KafkaListener` | `blocking-kafka` CRITICAL :9 | `VIBE-006` CRITICAL :9 |
| `KafkaConsumerBug.java` | `@KafkaListener` without `groupId` | `kafka` WARNING :7 | `VIBE-006` WARNING :8 |
| `KafkaConsumerBug.java` | no `@RetryableTopic` / DLQ | `kafka` WARNING :7 | — |
| `OrderService.java` | `future.get()` (blocking) inside `@Async` | `blocking` CRITICAL :22 | — (no MCP rule covers it) |
| `OrderService.java` | N+1: one repository call per loop iteration | — (no CLI rule) | `VIBE-003` CRITICAL :20 |

Notes:

- `VIBE-002` is about blocking a **Reactor** pipeline inside a Spring bean. On `ReactiveController` it fires because the class is a `@RestController` that imports Reactor — the `@Async` on the method plays no part. (Earlier versions of this README attributed it to `@Async`, and also listed `Thread.sleep()` and `future.get()` under VIBE-002; neither is covered by that rule.)
- The CLI used to report the `.block()` twice (as `blocking` and `reactor-block`); 2.0.0 reports it once.
- MCP output above: `java-vibe-guard-mcp-2.0.0.jar` from the [v2.0.0 release](https://github.com/Joaquinriosheredia/java-vibe-guard/releases/tag/v2.0.0).

## Why no build tool?

java-vibe-guard is a **static text scanner** — it reads `.java` source files directly without compiling them.
Plain files are sufficient to trigger all rules.
