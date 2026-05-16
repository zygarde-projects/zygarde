# GraphQL Implementation Status

Last updated: 2026-05-17

## Current State

- GraphQL feasibility and design notes live in `doc/graphql-support-investigation.md`.
- `modules-web/zygarde-web-codegen` contains the initial `GraphQlApiGenerator` and GraphQL generation value objects.
- `modules-web/zygarde-graphql-codegen-dsl` contains the first DSL entry point for query, mutation, type, and input declarations.
- `samples/todo-multimodule-dsl` has a generated Todo GraphQL sample plus handwritten Book/Author GraphQL coverage for relation filtering and `@BatchMapping`.

## Changed In This Round

- Added collection argument support to the GraphQL generation model, API generator, and DSL.
- `collectionArgument<T>(...)` now generates Kotlin `Collection<T>` resolver/service parameters and SDL list arguments such as `[Int!]!`.
- Nullable collection arguments and nullable list items are supported, including Kotlin `Collection<T?>?` and SDL `[T]`.
- Added focused generator and DSL tests for collection argument behavior.

## Validation

- `rtk ./gradlew -p modules-web/zygarde-web-codegen test` - passed.
- `rtk ./gradlew -p modules-web/zygarde-graphql-codegen-dsl test` - passed.
- `rtk ./gradlew -p modules-web/zygarde-web-codegen ktlintCheck` - passed.
- `rtk ./gradlew -p modules-web/zygarde-graphql-codegen-dsl ktlintCheck` - passed.

## Next Work

- Add nullable response documentation to user-facing GraphQL DSL docs once those docs are introduced or expanded.
- Consider argument default values and input field nullability ergonomics in the DSL, especially for filter inputs.
- Consider adding `collectionField<T>(...)` KClass/reified overloads to match `collectionArgument<T>(...)` ergonomics.
- Consider using collection arguments in the Todo sample, for example an `ids` filter/list query, if sample coverage should demonstrate this feature end to end.
- Decide whether generated schemas should remain split per DSL schema or eventually be aggregated into one schema artifact.
- Continue toward automatic SDL generation from model-mapping metadata so generated GraphQL types do not need to be duplicated manually in DSL declarations.

## Blockers And Context

- No active blockers.
- GraphQL runtime support is still sample/codegen focused; no dedicated `zygarde-graphql` runtime module exists yet.
- Error handling, authentication context injection, custom scalar registration, pagination shape, and generated DataLoader/batch resolver support remain open design and implementation areas.
