# java-vibe-guard-demo

Minimal reproducible demo for [java-vibe-guard](https://github.com/Joaquinriosheredia/java-vibe-guard).

This project contains exactly **3 plain `.java` files** that produce **3 CRITICAL findings** when scanned with the CLI — no Maven, no Gradle, no runtime dependencies.

## Run

```bash
npx java-vibe-guard@2.1.0 .
```

Exit code `1` (CRITICAL findings present).

## Expected output (java-vibe-guard 2.1.0)

Real output, captured with `npx java-vibe-guard@2.1.0 . --no-color`:

```
java-vibe-guard — vibe coding detector for Java/Spring Boot
Scanning: .  (3 files)

❌ CRITICAL: Thread.sleep() detected in @KafkaListener method → src/main/java/demo/KafkaConsumerBug.java:9
  Evidence (measured, java-vibe-guard verify/blocking-kafka — @KafkaListener): the damage appears only when max.poll.records x time per record > max.poll.interval.ms (with the defaults, 500 records and 300 s: more than 600 ms per record). Above it, in an accelerated setup (max.poll.interval.ms lowered to 10 s), the consumer left the group on every batch, every offset commit failed and the group entered a reprocessing loop: 0 records/s committed, each record delivered ~10 times. Below it, the same blocking call caused no measured damage: no rebalances, no duplicates, throughput = consumers / time per record. Measured on kafka-clients 3.6.2, classic group protocol with eager rebalancing, spring-kafka AckMode BATCH; the cooperative protocol, KIP-848 and AckMode RECORD were not measured
  Source: https://github.com/Joaquinriosheredia/java-vibe-guard/blob/a6f32ef/cli/verify/blocking-kafka/results/criteria.md#L7-L47
❌ CRITICAL: blocking Future.get() detected in @Async method → src/main/java/demo/OrderService.java:22
  Evidence (measured, java-vibe-guard verify/blocking — @Async): on Spring Boot's default @Async executor (8 platform threads, unbounded queue), a call that holds the thread caps throughput at threads / call duration: 39.9-40.0 tasks/s with 8 threads, 79.8 with 16. Above that the queue grows at (load - capacity) and 98-99.5% of latency is queue wait; the same call without holding the thread did not queue. With virtual threads enabled (spring.threads.virtual.enabled=true) the executor did not saturate
  Source: https://github.com/Joaquinriosheredia/java-vibe-guard/blob/c4e5ddd/cli/verify/blocking/results/criteria.md#L7-L38
  Evidence (@Scheduled, @EventListener): documented mechanism, no benchmark of our own — the call holds a thread of the scheduler / event pool for its whole duration; under load the pool saturates
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

The `Evidence` lines show what each rule rests on:

- **`blocking-kafka`** and **`blocking` under `@Async`** cite measured results from pre-registered experiments in the java-vibe-guard repo (`cli/verify/`). The `Source:` line points to the results, pinned to a commit.
- `blocking` under `@Scheduled` / `@EventListener` (not present here) and `reactor-block` have no benchmark of their own yet. The output says "documented mechanism", instead of borrowing a figure from a benchmark that measured something else.

Since 2.1.0, `blocking` under `@Async` is reported as WARNING when the module's base `application.properties` / `application.yml` sets `spring.threads.virtual.enabled=true`. This demo has no build file or Spring config, so it stays CRITICAL.

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
