# GraphQL Implementation Status

Last updated: 2026-05-17 (`union` type support round)

## Current State

- GraphQL feasibility and design notes live in `doc/graphql-support-investigation.md`.
- `modules-web/zygarde-web-codegen` contains the `GraphQlApiGenerator` and GraphQL generation value objects, including query, mutation, subscription, object type, input, enum, scalar, **union**, nullable collection, raw SDL default-value, SDL descriptions (operations, type definitions, `type`/`input` fields, operation arguments, enum values), and `@deprecated` directives on every GraphQL position (operation fields, operation arguments, `type`/`input` fields, enum values).
- `modules-web/zygarde-graphql-codegen-dsl` contains the DSL entry point for query, mutation, subscription, type, input, enum, scalar, and **union** declarations, including Kotlin enum value derivation helpers, `GraphQlDefaultValue` helpers, descriptions, and `deprecationReason` support.
- `samples/todo-multimodule-dsl` has a generated Todo GraphQL sample plus handwritten Book/Author GraphQL coverage for relation filtering and `@BatchMapping`.

## Changed In This Round

- Added GraphQL `union` type support — the first composite SDL kind beyond `type`/`input`/`enum`/`scalar`.
- Production changes:
  - `GraphQlTypeDefinitionKind` gained a `UNION` entry; `GraphQlTypeDefinitionToGenerateVo` gained a `unionMemberTypes: MutableList<String>` property (defaults to empty).
  - `GraphQlApiGenerator.validateGraphQlNames` validates that a union declares at least one member type and that every member type is a valid GraphQL name. `validateUniqueGraphQlDeclarations` rejects duplicate union member types.
  - SDL rendering emits `union Name = A | B | C` (no braces, like `scalar`), with the optional type-definition description rendered as a block string before it. Union, like `scalar`, generates no controller/service/Kotlin artifacts — it is schema-only.
  - DSL: `DslGraphQlSchema.union(name, vararg memberTypes, description)` and an `Iterable<String>` overload. Both eagerly validate the type-definition name, name uniqueness across the schema, description blankness, non-empty members, each member name, and duplicate members.
  - `DslGraphQlTypeDefinition` (used only for `type`/`input`/`enumType`) gained `UNION` branches in its two `when (kind)` blocks to stay exhaustive; union is never constructed through it.
- Tests added:
  - Generator: `should render GraphQL union types in generated schema`, `should reject GraphQL union types without member types before rendering generated output`, `should reject invalid GraphQL union member type names before rendering generated output`, `should reject duplicate GraphQL union member types before rendering generated output`.
  - DSL: `should carry GraphQL union types through the DSL`, `should reject GraphQL union types without member types`, `should reject duplicate GraphQL union member types`.
- `doc/graphql-dsl-guide.md` updated: new `### union` section, the type-definition heading and `defaultGraphQlType` note now list `union`, the validation summary covers union uniqueness/non-empty rules, and the limitations section notes that `interface` is still unsupported and that union/interface `TypeResolver` wiring is still manual.
- `union` is purely additive SDL — existing generated sample output is unchanged, so no sample regeneration was needed.

## Validation

- `./gradlew :zygarde-web-codegen:test --tests zygarde.codegen.generator.GraphQlApiGeneratorTest :zygarde-graphql-codegen-dsl:test --tests zygarde.codegen.dsl.graphql.GraphQlDslCodegenTest :zygarde-web-codegen:ktlintCheck :zygarde-graphql-codegen-dsl:ktlintCheck` — `BUILD SUCCESSFUL`. Both test suites pass, including the seven new union tests.
- detekt is not wired to these modules' sources (prior rounds reported `NO-SOURCE`), so it was not re-run this round.

## Next Work

- Consider GraphQL `interface` support (object types `implements` an interface, plus interface field declarations) — the natural counterpart to `union` now that union exists.
- Optionally add a `union` (and a deprecated operation field / argument / enum value, plus the metadata directives) to the `samples/todo-multimodule-dsl` GraphQL DSL and regenerate to demonstrate the description / `@deprecated` / `union` features end-to-end in a real sample.
- Link `doc/graphql-dsl-guide.md` from a top-level docs index or README if/when a docs index exists.
- Consider generating enum SDL automatically from model-mapping metadata so users do not need to declare enum GraphQL types manually.
- Consider adding a generated sample subscription once the sample app has an event source worth exposing.
- Decide whether generated schemas should remain split per DSL schema or eventually be aggregated into one schema artifact.
- Continue toward automatic SDL generation from model-mapping metadata so generated GraphQL types do not need to be duplicated manually in DSL declarations.

## Blockers And Context

- No active blockers.
- `doc/graphql-dsl-guide.md` is the user-facing usage guide; `doc/graphql-support-investigation.md` remains the design/feasibility report and this file remains the round-to-round handoff.
- Schema-only GraphQL DSL declarations are useful for shared SDL fragments such as scalars, unions, and common object/input types; they avoid stale empty generated Kotlin files.
- GraphQL API/schema names must be unique across one generator invocation; duplicate names fail before output rendering to avoid colliding controller/service/schema artifacts.
- Empty `type`, `input`, and `enum` declarations are rejected in both the DSL and generator; use `scalar(...)` for fieldless scalar declarations and `union(...)` for type unions.
- `union` renders as `union Name = A | B` and, like `scalar`, produces no Kotlin controller/service artifacts; member types must be valid GraphQL names, must be non-empty, and must not repeat. The generator does not verify that member types resolve to declared object types, and it does not generate a runtime `TypeResolver` — union/interface type resolution must still be wired manually via `RuntimeWiringConfigurer`.
- Default values are supported only for GraphQL input fields and operation arguments; both DSL and direct generator VO usage fail fast for object type field defaults.
- GraphQL fields with the same name are valid across different operation roots; generated Spring mapping annotations keep the GraphQL name stable even when Kotlin method names need operation prefixes.
- GraphQL names that are Kotlin reserved identifiers are valid SDL names; generated Kotlin declarations are handled by KotlinPoet, and generated call sites escape those references explicitly.
- GraphQL name validation follows the GraphQL lexical name grammar (`[_A-Za-z][_0-9A-Za-z]*`) and rejects the reserved `__` introspection-name prefix.
- Generated Spring GraphQL bindings carry explicit annotation names, so runtime field/argument binding is not dependent on reflected Kotlin method or parameter names.
- GraphQL descriptions render as block strings (`"""..."""`); operations, type definitions, `type`/`input` fields, operation arguments, and enum values all carry them, and a set-but-blank description fails fast.
- Operation arguments render inline (`(name: Type)`) until at least one argument carries a description, at which point the whole argument list switches to multi-line rendering so each argument can carry a block string. `@deprecated` on arguments does not trigger multi-line rendering because the reason is a single-line literal.
- `@deprecated` is supported on every operation/object/enum position; the GraphQL spec forbids it on required input fields and required arguments (non-null without a default), enforced by both the DSL and generator. A set-but-blank `deprecationReason` fails fast.
- `enumType<T>()` derives values from a Kotlin enum and cannot carry per-value descriptions or deprecation reasons; the manual `value(name, description, deprecationReason)` form is required for those.
- GraphQL runtime support is still sample/codegen focused; no dedicated `zygarde-graphql` runtime module exists yet.
- GraphQL `interface` types are not yet supported; error handling, authentication context injection, custom scalar registration, pagination shape, and generated DataLoader/batch resolver support remain open design and implementation areas.
