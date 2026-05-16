# GraphQL Implementation Status

Last updated: 2026-05-17

## Current State

- GraphQL feasibility and design notes live in `doc/graphql-support-investigation.md`.
- `modules-web/zygarde-web-codegen` contains the initial `GraphQlApiGenerator` and GraphQL generation value objects.
- `modules-web/zygarde-graphql-codegen-dsl` contains the first DSL entry point for query, mutation, type, and input declarations.
- `samples/todo-multimodule-dsl` has a generated Todo GraphQL sample plus handwritten Book/Author GraphQL coverage for relation filtering and `@BatchMapping`.

## Changed In This Round

- Extended the Todo GraphQL DSL sample with a generated `todosByIds(ids: [Int!]!)` query.
- Regenerated the Todo GraphQL controller, service interface, and SDL so `collectionArgument<Int>("ids")` is covered end to end in sample output.
- Implemented `TodoGraphQlServiceImpl.todosByIds(...)` with the existing JPA search `inList` operator.
- Added Spring GraphQL sample test coverage that queries `todosByIds` and verifies only the requested todo is returned.

## Validation

- `rtk ./gradlew :todo-codegen-dsl-graphql:run` - passed.
- `rtk ./gradlew :todo-src-main:test --tests example.test.TodoGraphQlTest` - passed.
- `rtk ./gradlew :todo-codegen-dsl-graphql:ktlintCheck :todo-src-main:ktlintCheck` - passed.

## Next Work

- Add nullable response documentation to user-facing GraphQL DSL docs once those docs are introduced or expanded.
- Consider argument default values and input field nullability ergonomics in the DSL, especially for filter inputs.
- Consider using collection fields in sample SDL if a future sample model needs list-valued GraphQL fields.
- Consider adding input-object collection field runtime coverage, for example an `idsIn` field on `TodoFilter`, now that top-level collection arguments are covered.
- Decide whether generated schemas should remain split per DSL schema or eventually be aggregated into one schema artifact.
- Continue toward automatic SDL generation from model-mapping metadata so generated GraphQL types do not need to be duplicated manually in DSL declarations.

## Blockers And Context

- No active blockers.
- In this environment, Gradle validation needed sandbox escalation because the wrapper writes lock files under `~/.gradle`.
- GraphQL runtime support is still sample/codegen focused; no dedicated `zygarde-graphql` runtime module exists yet.
- Error handling, authentication context injection, custom scalar registration, pagination shape, and generated DataLoader/batch resolver support remain open design and implementation areas.
