# GraphQL Implementation Status

Last updated: 2026-05-17 (description support round)

## Current State

- GraphQL feasibility and design notes live in `doc/graphql-support-investigation.md`.
- `modules-web/zygarde-web-codegen` contains the initial `GraphQlApiGenerator` and GraphQL generation value objects, including query, mutation, subscription, object type, input, enum, scalar, nullable collection, raw SDL default-value, and operation/type-definition description support.
- `modules-web/zygarde-graphql-codegen-dsl` contains the first DSL entry point for query, mutation, subscription, type, input, enum, and scalar declarations, including Kotlin enum value derivation helpers, `GraphQlDefaultValue` helpers for safer SDL default-value literals, and operation/type-definition descriptions.
- `samples/todo-multimodule-dsl` has a generated Todo GraphQL sample plus handwritten Book/Author GraphQL coverage for relation filtering and `@BatchMapping`.

## Changed In This Round

- Added GraphQL SDL description (`"""..."""`) support at the operation-field and type-definition level — the highest-value, prominently introspected subset of GraphQL descriptions.
- Production changes:
  - `GraphQlFunctionToGenerateVo` and `GraphQlTypeDefinitionToGenerateVo` gained a nullable `description` field.
  - `GraphQlName.kt` gained `requireGraphQlDescription`, which rejects a non-null but blank description.
  - `GraphQlApiGenerator` now validates descriptions before rendering and emits them as GraphQL block strings: single-line descriptions render inline as `"""text"""`, while multi-line descriptions (or ones ending in `"`) render as an indented multi-line block string; embedded `"""` is escaped to `\"""`.
  - DSL: `DslGraphQlFunction` and `DslGraphQlTypeDefinition` gained a `var description` property; `DslGraphQlSchema.scalar` (and its reified overload) gained a `description` parameter.
- Field-, argument-, and enum-value-level descriptions are intentionally out of scope this round: they would require touching ~12 `field`/`collectionField`/`argument`/`collectionArgument` overloads and reworking the enum-value representation from `MutableList<String>` to a VO. Deferred to keep this change scoped.
- Descriptions are opt-in and default to `null`, so existing generated sample output is unchanged and no sample regeneration was needed.

## Validation

- `./gradlew :zygarde-web-codegen:test --tests zygarde.codegen.generator.GraphQlApiGeneratorTest :zygarde-graphql-codegen-dsl:test --tests zygarde.codegen.dsl.graphql.GraphQlDslCodegenTest` - passed (both suites green, including the two new description tests per module).
- `./gradlew :zygarde-web-codegen:ktlintCheck :zygarde-graphql-codegen-dsl:ktlintCheck` - passed.
- `./gradlew :zygarde-web-codegen:detekt :zygarde-graphql-codegen-dsl:detekt` - `NO-SOURCE` (detekt is not wired to these modules' sources).

## Next Work

- Extend description support to individual `field` / `collectionField` / `argument` / `collectionArgument` declarations and to enum values; the enum-value part needs `enumValues` to become a VO list rather than `MutableList<String>`.
- Optionally add descriptions to the `samples/todo-multimodule-dsl` GraphQL DSL and regenerate to demonstrate the feature end-to-end.
- Link `doc/graphql-dsl-guide.md` from a top-level docs index or README if/when a docs index exists.
- Consider generating enum SDL automatically from model-mapping metadata so users do not need to declare enum GraphQL types manually.
- Consider adding a generated sample subscription once the sample app has an event source worth exposing.
- Consider using collection fields in sample SDL if a future sample model needs list-valued GraphQL fields.
- Decide whether generated schemas should remain split per DSL schema or eventually be aggregated into one schema artifact.
- Continue toward automatic SDL generation from model-mapping metadata so generated GraphQL types do not need to be duplicated manually in DSL declarations.

## Blockers And Context

- No active blockers.
- `doc/graphql-dsl-guide.md` is the user-facing usage guide; `doc/graphql-support-investigation.md` remains the design/feasibility report and this file remains the round-to-round handoff.
- Gradle validation required sandbox escalation in this round because the wrapper needed lock-file access under `~/.gradle`.
- Schema-only GraphQL DSL declarations are useful for shared SDL fragments such as scalars and common object/input types; they now avoid stale empty generated Kotlin files.
- GraphQL API/schema names must be unique across one generator invocation; duplicate names now fail before output rendering to avoid colliding controller/service/schema artifacts.
- Empty `type`, `input`, and `enum` declarations are now rejected in both the DSL and generator; use `scalar(...)` for fieldless scalar declarations.
- Default values are supported only for GraphQL input fields; both DSL and direct generator VO usage now fail fast for object type field defaults.
- GraphQL fields with the same name are valid across different operation roots; generated Spring mapping annotations keep the GraphQL name stable even when Kotlin method names need operation prefixes.
- GraphQL names that are Kotlin reserved identifiers are valid SDL names; generated Kotlin declarations are handled by KotlinPoet, and generated call sites now escape those references explicitly.
- GraphQL name validation now follows the GraphQL lexical name grammar (`[_A-Za-z][_0-9A-Za-z]*`) and rejects the reserved `__` introspection-name prefix.
- Generated Spring GraphQL bindings now carry explicit annotation names, so runtime field/argument binding is no longer dependent on reflected Kotlin method or parameter names.
- Scalar SDL declarations are now generated, but runtime scalar registration remains separate; custom scalar coercing still needs Spring GraphQL/GraphQL Java configuration.
- GraphQL descriptions render as block strings (`"""..."""`); only operation fields and type definitions carry them so far, and a set-but-blank description fails fast.
- GraphQL runtime support is still sample/codegen focused; no dedicated `zygarde-graphql` runtime module exists yet.
- Error handling, authentication context injection, custom scalar registration, pagination shape, and generated DataLoader/batch resolver support remain open design and implementation areas.
