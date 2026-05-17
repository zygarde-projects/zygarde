# GraphQL Implementation Status

Last updated: 2026-05-17

## Current State

- GraphQL feasibility and design notes live in `doc/graphql-support-investigation.md`.
- `modules-web/zygarde-web-codegen` contains the initial `GraphQlApiGenerator` and GraphQL generation value objects, including query, mutation, subscription, object type, input, enum, nullable collection, and raw SDL default-value support.
- `modules-web/zygarde-graphql-codegen-dsl` contains the first DSL entry point for query, mutation, subscription, type, input, and enum declarations.
- `samples/todo-multimodule-dsl` has a generated Todo GraphQL sample plus handwritten Book/Author GraphQL coverage for relation filtering and `@BatchMapping`.

## Changed In This Round

- Added `GraphQlOperation.SUBSCRIPTION`.
- Generated subscription resolver methods now use Spring GraphQL `@SubscriptionMapping`.
- Generated SDL now emits `type Subscription` for the first subscription root and `extend type Subscription` for later generated schemas.
- Added the GraphQL DSL `subscription("...") { ... }` schema builder entry point.
- Added generator and DSL tests covering subscription codegen, service interface methods, SDL output, and operation-root extension behavior.

## Validation

- `rtk ./gradlew :zygarde-web-codegen:ktlintFormat :zygarde-graphql-codegen-dsl:ktlintFormat` - passed.
- `rtk ./gradlew :zygarde-web-codegen:test --tests zygarde.codegen.generator.GraphQlApiGeneratorTest :zygarde-graphql-codegen-dsl:test --tests zygarde.codegen.dsl.graphql.GraphQlDslCodegenTest` - passed.
- `rtk ./gradlew :zygarde-web-codegen:ktlintCheck :zygarde-graphql-codegen-dsl:ktlintCheck` - passed.

## Next Work

- Add nullable response documentation to user-facing GraphQL DSL docs once those docs are introduced or expanded.
- Add user-facing docs for `subscription(...)` and clarify that callers must choose an appropriate Kotlin return type, such as a reactive publisher type, when wiring real Spring GraphQL subscriptions.
- Consider adding safer typed helpers for GraphQL default values so callers do not need to pass raw SDL literals.
- Consider generating enum SDL automatically from model-mapping metadata or Kotlin enum types so users do not need to duplicate enum values manually.
- Consider adding a generated sample subscription once the sample app has an event source worth exposing.
- Consider using collection fields in sample SDL if a future sample model needs list-valued GraphQL fields.
- Decide whether generated schemas should remain split per DSL schema or eventually be aggregated into one schema artifact.
- Continue toward automatic SDL generation from model-mapping metadata so generated GraphQL types do not need to be duplicated manually in DSL declarations.

## Blockers And Context

- No active blockers.
- In this environment, Gradle validation needed sandbox escalation because the wrapper writes lock files under `~/.gradle`.
- GraphQL runtime support is still sample/codegen focused; no dedicated `zygarde-graphql` runtime module exists yet.
- Error handling, authentication context injection, custom scalar registration, pagination shape, and generated DataLoader/batch resolver support remain open design and implementation areas.
