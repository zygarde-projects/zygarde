# GraphQL Implementation Status

Last updated: 2026-05-17 (argument description support round)

## Current State

- GraphQL feasibility and design notes live in `doc/graphql-support-investigation.md`.
- `modules-web/zygarde-web-codegen` contains the initial `GraphQlApiGenerator` and GraphQL generation value objects, including query, mutation, subscription, object type, input, enum, scalar, nullable collection, raw SDL default-value, operation/type-definition description, `type`/`input` field description, and operation-argument description support.
- `modules-web/zygarde-graphql-codegen-dsl` contains the first DSL entry point for query, mutation, subscription, type, input, enum, and scalar declarations, including Kotlin enum value derivation helpers, `GraphQlDefaultValue` helpers for safer SDL default-value literals, operation/type-definition descriptions, `field`/`collectionField` descriptions, and `argument`/`collectionArgument` descriptions.
- `samples/todo-multimodule-dsl` has a generated Todo GraphQL sample plus handwritten Book/Author GraphQL coverage for relation filtering and `@BatchMapping`.

## Changed In This Round

- Extended GraphQL SDL description support to individual operation arguments, completing the operation / type-definition / field / argument description arc (only enum-value descriptions remain).
- Production changes:
  - `GraphQlArgumentToGenerateVo` gained a nullable `description` field.
  - `GraphQlApiGenerator.validateGraphQlNames` now validates each operation-argument description (rejecting a non-null but blank value) with the label `GraphQL <operation> field '<fieldName>' argument '<argumentName>'`.
  - `GraphQlApiGenerator.toSchemaArguments` now takes the enclosing field indent and renders arguments multi-line whenever any argument carries a description: each argument lands on its own line at `fieldIndent + 2 spaces`, preceded by its block-string description, with the closing `)` aligned to the field indent. When no argument has a description the rendering stays inline (`(name: Type)`), so existing output is byte-for-byte unchanged. The shared `toSchemaDescription` block-string / escaping logic is reused.
  - DSL: all six `DslGraphQlFunction.argument` / `collectionArgument` overloads (raw `KClass`, `TypeName`, reified) gained a `description` parameter; the four base overloads validate the description eagerly via a private `requireArgumentDescription` helper.
- Argument descriptions are opt-in and default to `null`, so existing generated sample output is unchanged and no sample regeneration was needed.
- Enum-value-level descriptions remain the only deferred description case: `enumValues` is still a `MutableList<String>` and would need to become a VO list to carry per-value descriptions.

## Validation

- `./gradlew :zygarde-web-codegen:test --tests zygarde.codegen.generator.GraphQlApiGeneratorTest :zygarde-graphql-codegen-dsl:test --tests zygarde.codegen.dsl.graphql.GraphQlDslCodegenTest :zygarde-web-codegen:ktlintCheck :zygarde-graphql-codegen-dsl:ktlintCheck` - `BUILD SUCCESSFUL`. Both test suites pass, including the two new generator tests (render multi-line argument descriptions / reject blank argument description) and the extended DSL tests (`argument`/`collectionArgument` descriptions carried through and blank argument description rejected).
- detekt is not wired to these modules' sources (prior rounds reported `NO-SOURCE`), so it was not re-run this round.

## Next Work

- Add enum-value descriptions; this needs `enumValues` to become a VO list rather than `MutableList<String>`, plus ripple changes through the DSL `value()`/`values<T>()` helpers, generator validation, duplicate detection, and rendering.
- Optionally add operation-argument / field descriptions to the `samples/todo-multimodule-dsl` GraphQL DSL and regenerate to demonstrate the feature end-to-end.
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
- GraphQL descriptions render as block strings (`"""..."""`); operation fields, type definitions, `type`/`input` fields, and operation arguments carry them so far, and a set-but-blank description fails fast.
- Operation arguments render inline (`(name: Type)`) until at least one argument carries a description, at which point the whole argument list switches to multi-line rendering so each argument can carry a block string.
- GraphQL runtime support is still sample/codegen focused; no dedicated `zygarde-graphql` runtime module exists yet.
- Error handling, authentication context injection, custom scalar registration, pagination shape, and generated DataLoader/batch resolver support remain open design and implementation areas.
