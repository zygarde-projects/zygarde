# GraphQL Implementation Status

Last updated: 2026-05-17

## Current State

- GraphQL feasibility and design notes live in `doc/graphql-support-investigation.md`.
- `modules-web/zygarde-web-codegen` contains the initial `GraphQlApiGenerator` and GraphQL generation value objects.
- `modules-web/zygarde-graphql-codegen-dsl` contains the first DSL entry point for query, mutation, type, and input declarations.
- `samples/todo-multimodule-dsl` has a generated Todo GraphQL sample plus handwritten Book/Author GraphQL coverage for relation filtering and `@BatchMapping`.

## Changed In This Round

- Fixed generated SDL for multiple GraphQL DSL schemas in the same application.
- The first generated root operation declaration still emits `type Query` or `type Mutation`.
- Later generated schemas with the same root operation now emit `extend type Query` or `extend type Mutation`, avoiding duplicate root type definitions when Spring GraphQL loads several generated `.graphqls` files.
- Added generator coverage for the multi-schema root operation behavior.

## Validation

- `./gradlew -p modules-web/zygarde-web-codegen test`
- Result: passed.

## Next Work

- Regenerate sample GraphQL outputs after DSL/API behavior changes when the sample DSL uses multiple generated schemas.
- Add response nullability support to the DSL and generated service/controller signatures if nullable object lookups are needed.
- Decide whether generated schemas should remain split per DSL schema or eventually be aggregated into one schema artifact.
- Continue toward automatic SDL generation from model-mapping metadata so generated GraphQL types do not need to be duplicated manually in DSL declarations.

## Blockers And Context

- No active blockers.
- GraphQL runtime support is still sample/codegen focused; no dedicated `zygarde-graphql` runtime module exists yet.
- Error handling, authentication context injection, custom scalar registration, pagination shape, and generated DataLoader/batch resolver support remain open design and implementation areas.
