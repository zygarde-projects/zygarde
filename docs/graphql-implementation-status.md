# GraphQL Implementation Status

Last updated: 2026-05-17 (field `@deprecated` directive round)

## Current State

- GraphQL feasibility and design notes live in `doc/graphql-support-investigation.md`.
- `modules-web/zygarde-web-codegen` contains the `GraphQlApiGenerator` and GraphQL generation value objects, including query, mutation, subscription, object type, input, enum, scalar, nullable collection, raw SDL default-value, SDL descriptions (operations, type definitions, `type`/`input` fields, operation arguments, enum values), and `@deprecated` directives on `type`/`input` fields.
- `modules-web/zygarde-graphql-codegen-dsl` contains the DSL entry point for query, mutation, subscription, type, input, enum, and scalar declarations, including Kotlin enum value derivation helpers, `GraphQlDefaultValue` helpers for safer SDL default-value literals, descriptions for operations / type definitions / `field` / `collectionField` / `argument` / `collectionArgument` / `value`, and `deprecationReason` for `field` / `collectionField`.
- `samples/todo-multimodule-dsl` has a generated Todo GraphQL sample plus handwritten Book/Author GraphQL coverage for relation filtering and `@BatchMapping`.

## Changed In This Round

- Added GraphQL `@deprecated` directive support for object-type and input fields — the first deprecation feature, mirroring the earlier description arc.
- Production changes:
  - `GraphQlFieldToGenerateVo` gained a `deprecationReason: String?` property (defaults to `null`).
  - New shared `graphQlStringLiteral(value)` helper in `zygarde.codegen.model.graphql` (`GraphQlSchemaLiteral.kt`) renders a single-line escaped GraphQL string literal. `GraphQlDefaultValue.string` now delegates to it (behavior unchanged), removing duplicated escaping logic.
  - New `requireGraphQlDeprecationReason(reason, label)` validator in `GraphQlName.kt` rejects a non-null but blank reason.
  - `GraphQlApiGenerator.validateGraphQlNames` validates each field's `deprecationReason` and rejects deprecating a *required* input field (non-null with no default value) — GraphQL forbids `@deprecated` on required input fields. Object-type fields have no such restriction.
  - Field SDL rendering appends `@deprecated(reason: "...")` after the type and any input default value; the reason is escaped via `graphQlStringLiteral`. A field with no `deprecationReason` renders byte-for-byte as before.
  - DSL: `DslGraphQlTypeDefinition.field` / `collectionField` (all three overloads each) gained an optional `deprecationReason` parameter, validated eagerly via a shared `requireFieldDeprecation` helper (blank check + required-input-field check).
- Tests added:
  - Generator: `should render GraphQL field deprecation in generated schema` (covers object type, nullable input field, input field with default value, and reason escaping), `should reject blank GraphQL field deprecation reasons before rendering generated output`, `should reject deprecation on required GraphQL input fields before rendering generated output`.
  - DSL: `should carry GraphQL field deprecation reasons through the DSL`, `should reject blank GraphQL field deprecation reasons`, `should reject deprecation on required GraphQL input fields`.
- `doc/graphql-dsl-guide.md` updated: new "棄用標記(GraphQL `@deprecated`)" section, a deprecation bullet in the validation summary, and the limitations/coverage section now records that only `type`/`input` field deprecation is supported.
- Field deprecation is opt-in and defaults to `null`, so existing generated sample output is unchanged and no sample regeneration was needed.

## Validation

- `./gradlew :zygarde-web-codegen:test --tests zygarde.codegen.generator.GraphQlApiGeneratorTest :zygarde-graphql-codegen-dsl:test --tests zygarde.codegen.dsl.graphql.GraphQlDslCodegenTest :zygarde-web-codegen:ktlintCheck :zygarde-graphql-codegen-dsl:ktlintCheck` — `BUILD SUCCESSFUL`. Both test suites pass, including the six new deprecation tests; `GraphQlDefaultValue` literal tests still pass after the `string` delegation.
- detekt is not wired to these modules' sources (prior rounds reported `NO-SOURCE`), so it was not re-run this round.

## Next Work

- Extend `@deprecated` to the remaining GraphQL positions: operation fields (`GraphQlFunctionToGenerateVo`) and operation arguments (`GraphQlArgumentToGenerateVo`) — both are FIELD/ARGUMENT definitions — and enum values (`GraphQlEnumValueToGenerateVo`). The required-argument rule (no `@deprecated` on non-null arguments without a default) mirrors the input-field rule already implemented here.
- Optionally add operation-argument / field / enum-value descriptions and a deprecated field to the `samples/todo-multimodule-dsl` GraphQL DSL and regenerate to demonstrate the metadata features end-to-end.
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
- `@deprecated` renders only on `type`/`input` fields so far. The reason is a single-line escaped string literal produced by the shared `graphQlStringLiteral` helper. The GraphQL spec forbids `@deprecated` on required input fields (non-null without a default); both the DSL and generator enforce this. A set-but-blank `deprecationReason` fails fast.
- `enumType<T>()` derives values from a Kotlin enum and cannot carry per-value descriptions; the manual `value(name, description)` form is required for those.
- GraphQL runtime support is still sample/codegen focused; no dedicated `zygarde-graphql` runtime module exists yet.
- Error handling, authentication context injection, custom scalar registration, pagination shape, and generated DataLoader/batch resolver support remain open design and implementation areas.
