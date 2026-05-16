# GraphQL Implementation Status

Last updated: 2026-05-17

## Current State

- GraphQL feasibility and design notes live in `doc/graphql-support-investigation.md`.
- `modules-web/zygarde-web-codegen` contains the initial `GraphQlApiGenerator` and GraphQL generation value objects.
- `modules-web/zygarde-graphql-codegen-dsl` contains the first DSL entry point for query, mutation, type, and input declarations.
- `samples/todo-multimodule-dsl` has a generated Todo GraphQL sample plus handwritten Book/Author GraphQL coverage for relation filtering and `@BatchMapping`.

## Changed In This Round

- Added nullable GraphQL response support to the DSL and generated API model.
- `returns(..., nullable = true)` now generates nullable Kotlin controller/service return types and nullable SDL object/scalar responses.
- `returnsCollection(..., nullable = true, itemNullable = true)` now generates nullable Kotlin collections/items and matching SDL list/item nullability.
- Regenerated the Todo GraphQL sample so the single `todo(id)` query is `Todo` instead of `Todo!`; the service now returns `null` for missing rows instead of throwing through `getById`.
- Added focused generator, DSL, and sample GraphQL tests for nullable response behavior.

## Validation

- `rtk ./gradlew -p modules-web/zygarde-web-codegen test` - passed.
- `rtk ./gradlew -p modules-web/zygarde-graphql-codegen-dsl test` - passed.
- `rtk ./gradlew -p samples/todo-multimodule-dsl/todo-codegen-dsl-graphql run` - passed; regenerated sample GraphQL artifacts.
- `rtk ./gradlew -p samples/todo-multimodule-dsl/todo-src-main test --tests example.test.TodoGraphQlTest` - passed.
- `rtk ./gradlew -p modules-web/zygarde-web-codegen ktlintCheck` - passed.
- `rtk ./gradlew -p modules-web/zygarde-graphql-codegen-dsl ktlintCheck` - passed.
- `rtk ./gradlew -p samples/todo-multimodule-dsl/todo-src-main ktlintCheck` - passed.
- `rtk ./gradlew -p samples/todo-multimodule-dsl/todo-dsl-generated-graphql-controller ktlintCheck` - passed.
- `rtk ./gradlew -p samples/todo-multimodule-dsl/todo-dsl-generated-graphql-service-interface ktlintCheck` - passed.

## Next Work

- Add nullable response documentation to user-facing GraphQL DSL docs once those docs are introduced or expanded.
- Consider argument default values and input field nullability ergonomics in the DSL, especially for filter inputs.
- Decide whether generated schemas should remain split per DSL schema or eventually be aggregated into one schema artifact.
- Continue toward automatic SDL generation from model-mapping metadata so generated GraphQL types do not need to be duplicated manually in DSL declarations.

## Blockers And Context

- No active blockers.
- GraphQL runtime support is still sample/codegen focused; no dedicated `zygarde-graphql` runtime module exists yet.
- Error handling, authentication context injection, custom scalar registration, pagination shape, and generated DataLoader/batch resolver support remain open design and implementation areas.
