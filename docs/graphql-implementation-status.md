# GraphQL Implementation Status

Last updated: 2026-05-17

## Current State

- GraphQL feasibility and design notes live in `doc/graphql-support-investigation.md`.
- `modules-web/zygarde-web-codegen` contains the initial `GraphQlApiGenerator` and GraphQL generation value objects, including query, mutation, subscription, object type, input, enum, scalar, nullable collection, and raw SDL default-value support.
- `modules-web/zygarde-graphql-codegen-dsl` contains the first DSL entry point for query, mutation, subscription, type, input, enum, and scalar declarations, including Kotlin enum value derivation helpers and `GraphQlDefaultValue` helpers for safer SDL default-value literals.
- `samples/todo-multimodule-dsl` has a generated Todo GraphQL sample plus handwritten Book/Author GraphQL coverage for relation filtering and `@BatchMapping`.

## Changed In This Round

- GraphQL generator now escapes Kotlin reserved identifiers when emitting controller-to-service calls, so legal GraphQL names such as field `class` and argument `in` generate compilable Kotlin references with backticked method and argument names.
- Added focused generator test coverage for GraphQL field and argument names that are legal SDL names but Kotlin keywords.

## Validation

- `./gradlew :zygarde-web-codegen:test --tests zygarde.codegen.generator.GraphQlApiGeneratorTest` - passed.
- `./gradlew :zygarde-web-codegen:ktlintCheck` - passed.

## Next Work

- Add user-facing GraphQL DSL docs for `GraphQlDefaultValue`, nullable responses, and collection nullability once those docs are introduced or expanded.
- Add user-facing docs for `subscription(...)` and clarify that callers must choose an appropriate Kotlin return type, such as a reactive publisher type, when wiring real Spring GraphQL subscriptions.
- Add user-facing docs for `scalar(...)` declarations and clarify that SDL declaration does not register GraphQL Java runtime coercing by itself.
- Consider generating enum SDL automatically from model-mapping metadata so users do not need to declare enum GraphQL types manually.
- Consider adding a generated sample subscription once the sample app has an event source worth exposing.
- Consider using collection fields in sample SDL if a future sample model needs list-valued GraphQL fields.
- Decide whether generated schemas should remain split per DSL schema or eventually be aggregated into one schema artifact.
- Continue toward automatic SDL generation from model-mapping metadata so generated GraphQL types do not need to be duplicated manually in DSL declarations.

## Blockers And Context

- No active blockers.
- Gradle validation ran locally without sandbox escalation in this round; earlier rounds may need escalation if the wrapper writes lock files under `~/.gradle`.
- Schema-only GraphQL DSL declarations are useful for shared SDL fragments such as scalars and common object/input types; they now avoid stale empty generated Kotlin files.
- Empty `type`, `input`, and `enum` declarations are now rejected in both the DSL and generator; use `scalar(...)` for fieldless scalar declarations.
- GraphQL fields with the same name are valid across different operation roots; generated Spring mapping annotations keep the GraphQL name stable even when Kotlin method names need operation prefixes.
- GraphQL names that are Kotlin reserved identifiers are valid SDL names; generated Kotlin declarations are handled by KotlinPoet, and generated call sites now escape those references explicitly.
- GraphQL name validation now follows the GraphQL lexical name grammar (`[_A-Za-z][_0-9A-Za-z]*`) and rejects the reserved `__` introspection-name prefix.
- Generated Spring GraphQL bindings now carry explicit annotation names, so runtime field/argument binding is no longer dependent on reflected Kotlin method or parameter names.
- Scalar SDL declarations are now generated, but runtime scalar registration remains separate; custom scalar coercing still needs Spring GraphQL/GraphQL Java configuration.
- GraphQL runtime support is still sample/codegen focused; no dedicated `zygarde-graphql` runtime module exists yet.
- Error handling, authentication context injection, custom scalar registration, pagination shape, and generated DataLoader/batch resolver support remain open design and implementation areas.
