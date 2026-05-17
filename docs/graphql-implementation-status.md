# GraphQL Implementation Status

Last updated: 2026-05-17 (operation field / argument / enum value `@deprecated` round)

## Current State

- GraphQL feasibility and design notes live in `doc/graphql-support-investigation.md`.
- `modules-web/zygarde-web-codegen` contains the `GraphQlApiGenerator` and GraphQL generation value objects, including query, mutation, subscription, object type, input, enum, scalar, nullable collection, raw SDL default-value, SDL descriptions (operations, type definitions, `type`/`input` fields, operation arguments, enum values), and `@deprecated` directives on **every** GraphQL position: operation fields, operation arguments, `type`/`input` fields, and enum values.
- `modules-web/zygarde-graphql-codegen-dsl` contains the DSL entry point for query, mutation, subscription, type, input, enum, and scalar declarations, including Kotlin enum value derivation helpers, `GraphQlDefaultValue` helpers for safer SDL default-value literals, descriptions for operations / type definitions / `field` / `collectionField` / `argument` / `collectionArgument` / `value`, and `deprecationReason` for operation functions / `argument` / `collectionArgument` / `field` / `collectionField` / `value`.
- `samples/todo-multimodule-dsl` has a generated Todo GraphQL sample plus handwritten Book/Author GraphQL coverage for relation filtering and `@BatchMapping`.

## Changed In This Round

- Completed the `@deprecated` arc — `@deprecated` is now supported on the three positions that were still missing: operation fields, operation arguments, and enum values.
- Production changes:
  - `GraphQlFunctionToGenerateVo`, `GraphQlArgumentToGenerateVo`, and `GraphQlEnumValueToGenerateVo` each gained a `deprecationReason: String?` property (defaults to `null`).
  - `GraphQlApiGenerator.validateGraphQlNames` now validates `deprecationReason` for operation fields, operation arguments, and enum values via the shared `requireGraphQlDeprecationReason` validator. It also rejects deprecating a *required* operation argument (non-null with no default value) — the same GraphQL spec rule already enforced for required input fields. Operation fields and enum values have no such restriction.
  - SDL rendering appends `@deprecated(reason: "...")` after the operation field response type, after each argument's type/default value (in both the inline and multi-line argument rendering paths), and after each enum value. The reason is escaped via the shared `graphQlStringLiteral` helper. A field/argument/value with no `deprecationReason` renders byte-for-byte as before.
  - Argument deprecation does not force the multi-line argument layout — only descriptions still do — because deprecation reasons render inline as a single directive.
  - DSL: `DslGraphQlFunction` gained a `deprecationReason` property (validated in `toGraphQlFunctionToGenerateVo`); all six `argument`/`collectionArgument` overloads gained an optional `deprecationReason` parameter validated eagerly via a new shared `requireArgumentDeprecation` helper (blank check + required-argument check). `DslGraphQlTypeDefinition.value` gained an optional `deprecationReason` parameter.
- Tests added:
  - Generator: `should render GraphQL operation field argument and enum value deprecation in generated schema` (covers operation field deprecation, inline argument deprecation with reason escaping, argument deprecation in the multi-line path, and enum value deprecation), `should reject deprecation on required GraphQL arguments before rendering generated output`, `should reject blank GraphQL operation field argument and enum value deprecation reasons before rendering generated output`.
  - DSL: `should carry GraphQL operation field argument and enum value deprecation reasons through the DSL`, `should reject deprecation on required GraphQL arguments`, `should reject blank GraphQL operation field argument and enum value deprecation reasons`.
- `doc/graphql-dsl-guide.md` updated: the "棄用標記(GraphQL `@deprecated`)" section now documents all positions with a combined example, the validation summary and limitations sections were updated, and the "operation field / argument / enum value 的 `@deprecated`" limitation bullet was removed since it is now implemented.
- All deprecation features are opt-in and default to `null`, so existing generated sample output is unchanged and no sample regeneration was needed.

## Validation

- `./gradlew :zygarde-web-codegen:test --tests zygarde.codegen.generator.GraphQlApiGeneratorTest :zygarde-graphql-codegen-dsl:test --tests zygarde.codegen.dsl.graphql.GraphQlDslCodegenTest :zygarde-web-codegen:ktlintCheck :zygarde-graphql-codegen-dsl:ktlintCheck` — `BUILD SUCCESSFUL`. Both test suites pass, including the six new deprecation tests.
- detekt is not wired to these modules' sources (prior rounds reported `NO-SOURCE`), so it was not re-run this round.

## Next Work

- Optionally add a deprecated operation field / argument / enum value and the metadata directives to the `samples/todo-multimodule-dsl` GraphQL DSL and regenerate to demonstrate the description + `@deprecated` features end-to-end in a real sample.
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
- Default values are supported only for GraphQL input fields and operation arguments; both DSL and direct generator VO usage fail fast for object type field defaults.
- GraphQL fields with the same name are valid across different operation roots; generated Spring mapping annotations keep the GraphQL name stable even when Kotlin method names need operation prefixes.
- GraphQL names that are Kotlin reserved identifiers are valid SDL names; generated Kotlin declarations are handled by KotlinPoet, and generated call sites escape those references explicitly.
- GraphQL name validation follows the GraphQL lexical name grammar (`[_A-Za-z][_0-9A-Za-z]*`) and rejects the reserved `__` introspection-name prefix.
- Generated Spring GraphQL bindings carry explicit annotation names, so runtime field/argument binding is not dependent on reflected Kotlin method or parameter names.
- GraphQL descriptions render as block strings (`"""..."""`); operations, type definitions, `type`/`input` fields, operation arguments, and enum values all carry them, and a set-but-blank description fails fast.
- Operation arguments render inline (`(name: Type)`) until at least one argument carries a description, at which point the whole argument list switches to multi-line rendering so each argument can carry a block string. `@deprecated` on arguments does not trigger multi-line rendering because the reason is a single-line literal.
- `@deprecated` is now supported on every GraphQL position (operation fields, operation arguments, `type`/`input` fields, enum values). The reason is a single-line escaped string literal produced by the shared `graphQlStringLiteral` helper. The GraphQL spec forbids `@deprecated` on required input fields and required arguments (non-null without a default); both the DSL and generator enforce this. Operation fields, object-type fields, and enum values have no required-position restriction. A set-but-blank `deprecationReason` fails fast.
- `enumType<T>()` derives values from a Kotlin enum and cannot carry per-value descriptions or deprecation reasons; the manual `value(name, description, deprecationReason)` form is required for those.
- GraphQL runtime support is still sample/codegen focused; no dedicated `zygarde-graphql` runtime module exists yet.
- Error handling, authentication context injection, custom scalar registration, pagination shape, and generated DataLoader/batch resolver support remain open design and implementation areas.
