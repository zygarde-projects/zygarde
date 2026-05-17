# GraphQL Implementation Status

Last updated: 2026-05-17 (enum-value description support round)

## Current State

- GraphQL feasibility and design notes live in `doc/graphql-support-investigation.md`.
- `modules-web/zygarde-web-codegen` contains the `GraphQlApiGenerator` and GraphQL generation value objects, including query, mutation, subscription, object type, input, enum, scalar, nullable collection, raw SDL default-value, and SDL descriptions for operations, type definitions, `type`/`input` fields, operation arguments, and individual enum values.
- `modules-web/zygarde-graphql-codegen-dsl` contains the DSL entry point for query, mutation, subscription, type, input, enum, and scalar declarations, including Kotlin enum value derivation helpers, `GraphQlDefaultValue` helpers for safer SDL default-value literals, and descriptions for operations, type definitions, `field`/`collectionField`, `argument`/`collectionArgument`, and `value` (enum values).
- `samples/todo-multimodule-dsl` has a generated Todo GraphQL sample plus handwritten Book/Author GraphQL coverage for relation filtering and `@BatchMapping`.

## Changed In This Round

- Completed the GraphQL SDL description arc: individual enum values can now carry descriptions, so descriptions are now supported at the operation / type-definition / field / argument / enum-value levels.
- Production changes:
  - New `GraphQlEnumValueToGenerateVo(name, description?)` VO; `GraphQlTypeDefinitionToGenerateVo.enumValues` changed from `MutableList<String>` to `MutableList<GraphQlEnumValueToGenerateVo>`.
  - `GraphQlApiGenerator.validateGraphQlNames` now validates each enum value name and its description (rejecting a non-null but blank value) with the label `GraphQL enum '<name>' value '<value>'`.
  - `GraphQlApiGenerator` duplicate detection maps `enumValues` to `.name` before `firstDuplicateOrNull()`; the existing `GraphQL enum '<name>' value '<value>' is already declared` message is unchanged.
  - Enum SDL rendering now prepends each value with `toSchemaDescription("  ")` (the same shared block-string / escaping logic), so a value with no description is rendered byte-for-byte as before.
  - DSL: `DslGraphQlTypeDefinition.value(name)` gained an optional `description` parameter, validated eagerly via `requireGraphQlDescription`. `enumValues` storage is now a `GraphQlEnumValueToGenerateVo` list. The reified `values<T>()` helper still derives all values from a Kotlin enum with `null` descriptions.
- Tests updated to the new VO shape; new tests added:
  - Generator: `should render GraphQL enum value descriptions in generated schema` and `should reject blank GraphQL enum value descriptions before rendering generated output`.
  - DSL: `should carry GraphQL enum value descriptions through the DSL`, plus an enum-value blank-description case added to `should reject blank GraphQL descriptions`.
- `doc/graphql-dsl-guide.md` updated: the `enumType` section shows `value(name, description)`, the description section lists the `enum` value level, and the "current limitations" section no longer lists enum-value descriptions as unsupported.
- Enum-value descriptions are opt-in and default to `null`, so existing generated sample output is unchanged and no sample regeneration was needed.

## Validation

- `./gradlew :zygarde-web-codegen:test --tests zygarde.codegen.generator.GraphQlApiGeneratorTest :zygarde-graphql-codegen-dsl:test --tests zygarde.codegen.dsl.graphql.GraphQlDslCodegenTest :zygarde-web-codegen:ktlintCheck :zygarde-graphql-codegen-dsl:ktlintCheck` — `BUILD SUCCESSFUL`. Both test suites pass, including the new enum-value-description generator and DSL tests.
- detekt is not wired to these modules' sources (prior rounds reported `NO-SOURCE`), so it was not re-run this round.

## Next Work

- Optionally add operation-argument / field / enum-value descriptions to the `samples/todo-multimodule-dsl` GraphQL DSL and regenerate to demonstrate the description features end-to-end.
- Link `doc/graphql-dsl-guide.md` from a top-level docs index or README if/when a docs index exists.
- Consider generating enum SDL automatically from model-mapping metadata so users do not need to declare enum GraphQL types manually.
- Consider adding a generated sample subscription once the sample app has an event source worth exposing.
- Decide whether generated schemas should remain split per DSL schema or eventually be aggregated into one schema artifact.
- Continue toward automatic SDL generation from model-mapping metadata so generated GraphQL types do not need to be duplicated manually in DSL declarations.

## Blockers And Context

- No active blockers.
- `doc/graphql-dsl-guide.md` is the user-facing usage guide; `doc/graphql-support-investigation.md` remains the design/feasibility report and this file remains the round-to-round handoff.
- Schema-only GraphQL DSL declarations are useful for shared SDL fragments such as scalars and common object/input types; they avoid stale empty generated Kotlin files.
- GraphQL API/schema names must be unique across one generator invocation; duplicate names fail before output rendering to avoid colliding controller/service/schema artifacts.
- Empty `type`, `input`, and `enum` declarations are rejected in both the DSL and generator; use `scalar(...)` for fieldless scalar declarations.
- Default values are supported only for GraphQL input fields; both DSL and direct generator VO usage fail fast for object type field defaults.
- GraphQL fields with the same name are valid across different operation roots; generated Spring mapping annotations keep the GraphQL name stable even when Kotlin method names need operation prefixes.
- GraphQL names that are Kotlin reserved identifiers are valid SDL names; generated Kotlin declarations are handled by KotlinPoet, and generated call sites escape those references explicitly.
- GraphQL name validation follows the GraphQL lexical name grammar (`[_A-Za-z][_0-9A-Za-z]*`) and rejects the reserved `__` introspection-name prefix.
- Generated Spring GraphQL bindings carry explicit annotation names, so runtime field/argument binding is not dependent on reflected Kotlin method or parameter names.
- GraphQL descriptions render as block strings (`"""..."""`); operations, type definitions, `type`/`input` fields, operation arguments, and enum values all carry them, and a set-but-blank description fails fast.
- Operation arguments render inline (`(name: Type)`) until at least one argument carries a description, at which point the whole argument list switches to multi-line rendering so each argument can carry a block string.
- `enumType<T>()` derives values from a Kotlin enum and cannot carry per-value descriptions; the manual `value(name, description)` form is required for those.
- GraphQL runtime support is still sample/codegen focused; no dedicated `zygarde-graphql` runtime module exists yet.
- Error handling, authentication context injection, custom scalar registration, pagination shape, and generated DataLoader/batch resolver support remain open design and implementation areas.
