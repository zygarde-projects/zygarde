# GraphQL Implementation Status

Last updated: 2026-05-17

## Current State

- GraphQL feasibility and design notes live in `doc/graphql-support-investigation.md`.
- `modules-web/zygarde-web-codegen` contains the initial `GraphQlApiGenerator` and GraphQL generation value objects, including query, mutation, subscription, object type, input, enum, nullable collection, and raw SDL default-value support.
- `modules-web/zygarde-graphql-codegen-dsl` contains the first DSL entry point for query, mutation, subscription, type, input, and enum declarations, including Kotlin enum value derivation helpers.
- `samples/todo-multimodule-dsl` has a generated Todo GraphQL sample plus handwritten Book/Author GraphQL coverage for relation filtering and `@BatchMapping`.

## Changed In This Round

- Added `enumType<MyEnum>()` to GraphQL DSL schemas so Kotlin enum constants can be emitted as GraphQL enum values without duplicating each value manually.
- Added `values<MyEnum>()` inside named GraphQL enum definitions for cases where callers need a custom GraphQL enum type name but still want values from a Kotlin enum.
- Updated the DSL test coverage to exercise Kotlin enum value derivation through the generated GraphQL API model.

## Validation

- `rtk ./gradlew :zygarde-graphql-codegen-dsl:ktlintFormat :zygarde-graphql-codegen-dsl:test --tests zygarde.codegen.dsl.graphql.GraphQlDslCodegenTest` - passed.
- `rtk ./gradlew :zygarde-graphql-codegen-dsl:ktlintCheck` - passed.

## Next Work

- Add nullable response documentation to user-facing GraphQL DSL docs once those docs are introduced or expanded.
- Add user-facing docs for `subscription(...)` and clarify that callers must choose an appropriate Kotlin return type, such as a reactive publisher type, when wiring real Spring GraphQL subscriptions.
- Consider adding safer typed helpers for GraphQL default values so callers do not need to pass raw SDL literals.
- Consider generating enum SDL automatically from model-mapping metadata so users do not need to declare enum GraphQL types manually.
- Consider adding a generated sample subscription once the sample app has an event source worth exposing.
- Consider using collection fields in sample SDL if a future sample model needs list-valued GraphQL fields.
- Decide whether generated schemas should remain split per DSL schema or eventually be aggregated into one schema artifact.
- Continue toward automatic SDL generation from model-mapping metadata so generated GraphQL types do not need to be duplicated manually in DSL declarations.

## Blockers And Context

- No active blockers.
- In this environment, Gradle validation needed sandbox escalation because the wrapper writes lock files under `~/.gradle`.
- GraphQL runtime support is still sample/codegen focused; no dedicated `zygarde-graphql` runtime module exists yet.
- Error handling, authentication context injection, custom scalar registration, pagination shape, and generated DataLoader/batch resolver support remain open design and implementation areas.
