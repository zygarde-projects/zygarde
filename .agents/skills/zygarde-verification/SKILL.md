---
name: zygarde-verification
description: Verify completed Zygarde changes that affect Kotlin code, tests, Gradle configuration, generators, or tracked generated samples. Use before handoff and scale checks from focused module tests to the full cross-module build.
---

# Zygarde change verification

Inspect the final diff first and choose the smallest checks that cover every affected boundary. Do not claim checks that were not run.

## Required checks

1. Run `./gradlew ktlintCheck` for Kotlin or Kotlin DSL changes. If it fails only on formatting, run `./gradlew ktlintFormat`, inspect its diff, and rerun the check.
2. Run focused tests for each changed module while iterating, using root project paths such as `./gradlew :zygarde-core:test`.
3. Run `./gradlew detekt` when publishable Kotlin modules changed.
4. Run `./gradlew build` when changes cross module boundaries, alter shared build configuration, affect code generation, or are being prepared for release.
5. For codegen changes, first complete `$zygarde-codegen`; ensure regenerated tracked samples have no unexplained diff and compile the affected consumer samples.

Documentation-only, ignore-only, and agent-instruction-only changes need structural checks rather than the Gradle suite. Validate links and paths, validate any changed skill with the skill validator, and inspect `git diff --check`.

At handoff, report the exact commands run, whether they passed, and any check intentionally omitted with the reason. Warnings are not failures, but call out new or actionable warnings.
