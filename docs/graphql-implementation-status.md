# GraphQL Implementation Status

Last updated: 2026-05-17

## Current State

- GraphQL feasibility and design notes live in `doc/graphql-support-investigation.md`.
- `modules-web/zygarde-web-codegen` contains the initial `GraphQlApiGenerator` and GraphQL generation value objects, including query, mutation, subscription, object type, input, enum, scalar, nullable collection, and raw SDL default-value support.
- `modules-web/zygarde-graphql-codegen-dsl` contains the first DSL entry point for query, mutation, subscription, type, input, enum, and scalar declarations, including Kotlin enum value derivation helpers and `GraphQlDefaultValue` helpers for safer SDL default-value literals.
- `samples/todo-multimodule-dsl` has a generated Todo GraphQL sample plus handwritten Book/Author GraphQL coverage for relation filtering and `@BatchMapping`.

## Changed In This Round

- Added duplicate GraphQL declaration validation before generated Kotlin/SDL output is rendered.
- `GraphQlApiGenerator` now rejects duplicate operation fields across generated schemas, duplicate type definitions, duplicate function arguments, duplicate object/input fields, and duplicate enum values.
- `modules-web/zygarde-graphql-codegen-dsl` now fails early when a DSL schema repeats an operation field or type definition, when a function repeats an argument, when a type/input repeats a field, or when an enum repeats a value.
- Added focused generator and DSL tests for duplicate GraphQL declarations.

## Validation

- `rtk ./gradlew :zygarde-web-codegen:ktlintFormat :zygarde-graphql-codegen-dsl:ktlintFormat` - passed after sandbox escalation for Gradle wrapper access to `~/.gradle`.
- `rtk ./gradlew :zygarde-web-codegen:ktlintCheck :zygarde-graphql-codegen-dsl:ktlintCheck` - passed after sandbox escalation for Gradle wrapper access to `~/.gradle`.
- `rtk ./gradlew :zygarde-web-codegen:test --tests zygarde.codegen.generator.GraphQlApiGeneratorTest :zygarde-graphql-codegen-dsl:test --tests zygarde.codegen.dsl.graphql.GraphQlDslCodegenTest` - passed after sandbox escalation for Gradle wrapper access to `~/.gradle`.

## Next Work

- Decide whether GraphQL names beginning with `__` should be rejected in Zygarde helpers as reserved introspection names.
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
- In this environment, Gradle validation needed sandbox escalation because the wrapper writes lock files under `~/.gradle`.
- GraphQL name validation currently follows the GraphQL lexical name grammar (`[_A-Za-z][_0-9A-Za-z]*`) and does not yet enforce the `__` introspection-name reservation.
- Scalar SDL declarations are now generated, but runtime scalar registration remains separate; custom scalar coercing still needs Spring GraphQL/GraphQL Java configuration.
- GraphQL runtime support is still sample/codegen focused; no dedicated `zygarde-graphql` runtime module exists yet.
- Error handling, authentication context injection, custom scalar registration, pagination shape, and generated DataLoader/batch resolver support remain open design and implementation areas.
