# GraphQL Implementation Status

Last updated: 2026-05-17

## Current State

- GraphQL feasibility and design notes live in `doc/graphql-support-investigation.md`.
- `modules-web/zygarde-web-codegen` contains the initial `GraphQlApiGenerator` and GraphQL generation value objects.
- `modules-web/zygarde-graphql-codegen-dsl` contains the first DSL entry point for query, mutation, type, and input declarations.
- `samples/todo-multimodule-dsl` has a generated Todo GraphQL sample plus handwritten Book/Author GraphQL coverage for relation filtering and `@BatchMapping`.

## Changed In This Round

- Added `collectionField(...)` KClass and reified DSL overloads for GraphQL type/input declarations.
- `collectionField<T>(...)` now mirrors `field<T>(...)` and `collectionArgument<T>(...)` ergonomics while preserving nullable list and nullable item flags.
- Added focused DSL coverage for collection field metadata and generator coverage for SDL list fields such as `[String!]!` and `[String]`.

## Validation

- `rtk ./gradlew -p modules-web/zygarde-web-codegen test` - passed.
- `rtk ./gradlew -p modules-web/zygarde-graphql-codegen-dsl test` - passed.
- `rtk ./gradlew -p modules-web/zygarde-web-codegen ktlintCheck` - passed.
- `rtk ./gradlew -p modules-web/zygarde-graphql-codegen-dsl ktlintCheck` - passed.

## Next Work

- Add nullable response documentation to user-facing GraphQL DSL docs once those docs are introduced or expanded.
- Consider argument default values and input field nullability ergonomics in the DSL, especially for filter inputs.
- Consider using collection arguments in the Todo sample, for example an `ids` filter/list query, if sample coverage should demonstrate this feature end to end.
- Consider using collection fields in sample SDL if a future sample model needs list-valued GraphQL fields.
- Decide whether generated schemas should remain split per DSL schema or eventually be aggregated into one schema artifact.
- Continue toward automatic SDL generation from model-mapping metadata so generated GraphQL types do not need to be duplicated manually in DSL declarations.

## Blockers And Context

- No active blockers.
- In this environment, Gradle validation needed sandbox escalation because the wrapper writes lock files under `~/.gradle`.
- GraphQL runtime support is still sample/codegen focused; no dedicated `zygarde-graphql` runtime module exists yet.
- Error handling, authentication context injection, custom scalar registration, pagination shape, and generated DataLoader/batch resolver support remain open design and implementation areas.
