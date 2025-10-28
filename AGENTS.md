# Repository Guidelines

## Project Structure & Module Organization
Zygarde is a multi-module Kotlin project managed with Gradle Kotlin DSL. Production code lives under each `modules-*` directory (for example `modules-core/zygarde-core`, `modules-web/zygarde-webmvc`), while reusable test fixtures are centralized in `modules-test-support`. Sample applications and generated outputs are kept in `samples/`, and long-form design notes in `doc/`. Follow the conventional layout of `src/main/kotlin` and `src/test/kotlin` inside every module when adding new packages.

## Build, Test, and Development Commands
- `./gradlew build` compiles all modules, executes the full unit-test suite, and reports coverage.
- `./gradlew test` runs the unit tests only; use `-p <module>` to scope the run (e.g. `./gradlew -p modules-core/zygarde-core test`).
- `./gradlew ktlintCheck` enforces formatting, while `./gradlew ktlintFormat` auto-formats safe changes.
- `./gradlew detekt` runs static analysis on publishable modules; keep the report clean before opening a PR.
- For sample validation, `./gradlew :samples:todo-legacy:bootRun` starts the reference application.

## Coding Style & Naming Conventions
Kotlin sources follow `.editorconfig`: two-space indentation, spaces over tabs, and a 150-character soft limit. Stick to Kotlin casing (camelCase members, PascalCase classes) and align package names under `zygarde.*` or the relevant sample namespace. Prefer explicit imports; wildcard imports are disabled by ktlint. Keep generated code isolated in dedicated `*-codegen` modules or the `doc/codegen-*` directories.

## Testing Guidelines
Modules rely on JUnit Platform with Kotest assertions and MockK; co-locate tests under `src/test/kotlin` mirroring the production package structure. Name test classes after the unit under test with a `Test` suffix and adopt Kotest `should`/`given` style for readability. Run `./gradlew test` before pushing; the build automatically follows with `jacocoTestReport` and `printCoverage`, so aim to maintain or raise coverage numbers when modifying core modules.

## Commit & Pull Request Guidelines
Commit messages use the Conventional Commits format (`feat:`, `fix:`, `chore:`) as seen in `git log`; keep scope descriptors meaningful and group related changes together. When opening a pull request, include a concise summary of behavior changes, reference any tracked issues, and document new configuration or migration steps in `doc/` or the relevant module README. Attach screenshots or logs when UI- or HTTP-facing modules change, and confirm that build, ktlint, detekt, and tests pass locally before requesting review.

## Code Generation & Tooling Tips
Code generation support lives in `modules-codegen-support` and related `*-codegen` modules. When touching DSLs or generators, regenerate the sample outputs in `samples/todo-multimodule-dsl` and verify differences in version control. Use `scripts/deps.sh` to refresh dependency locks when upgrading libraries, and keep publishing credentials externalized through the documented environment variables (`PUNI_NEXUS_*`, `ZYGARDE_GH_*`).
