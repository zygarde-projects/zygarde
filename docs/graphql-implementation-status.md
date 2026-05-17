# GraphQL Implementation Status

Last updated: 2026-05-17 (field description support round)

## Current State

- GraphQL feasibility and design notes live in `doc/graphql-support-investigation.md`.
- `modules-web/zygarde-web-codegen` contains the initial `GraphQlApiGenerator` and GraphQL generation value objects, including query, mutation, subscription, object type, input, enum, scalar, nullable collection, raw SDL default-value, operation/type-definition description, and `type`/`input` field description support.
- `modules-web/zygarde-graphql-codegen-dsl` contains the first DSL entry point for query, mutation, subscription, type, input, enum, and scalar declarations, including Kotlin enum value derivation helpers, `GraphQlDefaultValue` helpers for safer SDL default-value literals, operation/type-definition descriptions, and `field`/`collectionField` descriptions.
- `samples/todo-multimodule-dsl` has a generated Todo GraphQL sample plus handwritten Book/Author GraphQL coverage for relation filtering and `@BatchMapping`.

## Changed In This Round

- Extended GraphQL SDL description support to individual `type` / `input` fields, parallel to the operation-field and type-definition descriptions added in the previous round.
- Production changes:
  - `GraphQlFieldToGenerateVo` gained a nullable `description` field.
  - `GraphQlApiGenerator.validateGraphQlNames` now validates each `type`/`input` field description (rejecting a non-null but blank value) with the label `GraphQL <type|input> '<typeName>' field '<fieldName>'`.
  - `GraphQlApiGenerator.toSchemaGenerateResult` now renders field descriptions as GraphQL block strings before the field line, indented two spaces; the existing single-line vs. multi-line `toSchemaDescription` rendering and `"""` escaping are reused unchanged.
  - DSL: all six `DslGraphQlTypeDefinition.field` / `collectionField` overloads (raw, `KClass`, reified) gained a `description` parameter; the base overloads validate the description eagerly via `requireGraphQlDescription`.
- Argument-level and enum-value-level descriptions are still out of scope: arguments render inline as `(name: Type)` so per-argument descriptions need a multi-line argument rendering change, and enum-value descriptions still need `enumValues` to become a VO list rather than `MutableList<String>`. Both remain deferred to keep this change scoped.
- Field descriptions are opt-in and default to `null`, so existing generated sample output is unchanged and no sample regeneration was needed.

## Validation

- `./gradlew :zygarde-web-codegen:test --tests zygarde.codegen.generator.GraphQlApiGeneratorTest :zygarde-graphql-codegen-dsl:test --tests zygarde.codegen.dsl.graphql.GraphQlDslCodegenTest :zygarde-web-codegen:ktlintCheck :zygarde-graphql-codegen-dsl:ktlintCheck` - `BUILD SUCCESSFUL`. Both test suites pass, including the two new generator tests (render field descriptions / reject blank field description) and the extended DSL tests (`field`/`collectionField` descriptions carried through and blank field description rejected).
- detekt is not wired to these modules' sources (prior rounds reported `NO-SOURCE`), so it was not re-run this round.

## Next Work

- Add argument-level descriptions; this needs `toSchemaArguments` to optionally break arguments onto multiple lines so each can carry a block-string description.
- Add enum-value descriptions; this needs `enumValues` to become a VO list rather than `MutableList<String>`.
- Optionally add field descriptions to the `samples/todo-multimodule-dsl` GraphQL DSL and regenerate to demonstrate the feature end-to-end.
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
- Scalar SDL declarations are generated, but runtime scalar registration remains separate; custom scalar coercing still needs Spring GraphQL/GraphQL Java configuration.
- GraphQL descriptions render as block strings (`"""..."""`); operation fields, type definitions, and `type`/`input` fields carry them so far, and a set-but-blank description fails fast.
- GraphQL runtime support is still sample/codegen focused; no dedicated `zygarde-graphql` runtime module exists yet.
- Error handling, authentication context injection, custom scalar registration, pagination shape, and generated DataLoader/batch resolver support remain open design and implementation areas.
