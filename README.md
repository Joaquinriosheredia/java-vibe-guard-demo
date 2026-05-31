# java-vibe-guard-demo

Minimal reproducible demo for [java-vibe-guard](https://github.com/Joaquinriosheredia/java-vibe-guard).

This project contains exactly **3 plain `.java` files** that produce **3 CRITICAL issues** when scanned — no Maven, no Gradle, no runtime dependencies.

## Run

```bash
npx java-vibe-guard .
```

## Expected output

```
❌ CRITICAL: Thread.sleep() detected in @KafkaListener method  → KafkaConsumerBug.java:9
❌ CRITICAL: blocking .get() detected in @Async method         → OrderService.java:22
❌ CRITICAL: blocking .block() detected in @Async method       → ReactiveController.java:14
⚠️  WARNING: @KafkaListener without explicit groupId
⚠️  WARNING: @KafkaListener without @RetryableTopic or DLQ
⚠️  WARNING: Endpoint without structured logging

Summary: 3 critical · 3 warnings
```

## Files and bugs

| File | Bug | Rule |
|------|-----|------|
| `ReactiveController.java` | `Mono.just(...).block()` inside `@Async` method | VIBE-002 |
| `KafkaConsumerBug.java` | `Thread.sleep()` inside `@KafkaListener` + missing `groupId` | VIBE-002 / VIBE-006 |
| `OrderService.java` | `future.get()` (blocking) inside `@Async` + N+1 loop | VIBE-002 / VIBE-003 |

## Why no build tool?

java-vibe-guard is a **static text scanner** — it reads `.java` source files directly without compiling them.
Plain files are sufficient to trigger all rules.
