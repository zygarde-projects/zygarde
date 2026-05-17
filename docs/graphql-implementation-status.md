# GraphQL Implementation Status

Last updated: 2026-05-17 (`typeFrom` / `inputFrom` model-mapping derivation round)

## Current State

- GraphQL feasibility and design notes live in `doc/graphql-support-investigation.md`.
- `modules-web/zygarde-web-codegen` contains the `GraphQlApiGenerator` and GraphQL generation value objects, including query, mutation, subscription, object type, input, enum, scalar, union, nullable collection, raw SDL default-value, SDL descriptions, and `@deprecated` directives on every GraphQL position.
- `modules-web/zygarde-graphql-codegen-dsl` contains the DSL entry point for query, mutation, subscription, type, input, enum, scalar, and union declarations, plus **`typeFrom` / `inputFrom` derivation of `type` / `input` declarations directly from model-mapping DTO metadata**.
- `modules-model-mapping/zygarde-model-mapping-codegen-dsl` now exposes `DtoMetaResolver` / `ModelMappingMetadata` / `ResolvedDtoField` — the single source of truth for a DTO's resolved field shape, shared by the DTO class generator and the GraphQL deriver.
- `samples/todo-multimodule-dsl` has a generated Todo GraphQL sample plus handwritten Book/Author GraphQL coverage for relation filtering and `@BatchMapping`.

## Changed In This Round

Closed the long-standing gap "GraphQL types still have to be re-declared by hand in the DSL" — GraphQL `type` / `input` declarations can now be derived from model-mapping DTOs.

- **Shared metadata (model-mapping module)**:
  - New `zygarde.codegen.dsl.meta` package: `ResolvedDtoField` (name, raw element type, nullability, collection flag, `dtoRef`, auto-id flag, comment), `ModelMappingMetadata` (per-`CodegenDto` resolved field index, with `EMPTY`), and `DtoMetaResolver` (resolves `DtoFieldMapping`s into the above).
  - `DtoFieldMappingCodeGenerator.fieldType()` was refactored to delegate to `DtoMetaResolver.resolveFieldType` so the generated DTO class and its GraphQL type cannot drift. Pure extraction — existing DTO output is unchanged and all model-mapping tests still pass.
- **GraphQL derivation (graphql DSL module)**:
  - `GraphQlTypeMapper` — maps resolved Kotlin field types to GraphQL scalar names (built-ins mirror `defaultGraphQlType`); custom types registered via `mapScalar`.
  - `GraphQlDtoDeriver` — derives a `type` / `input` definition plus every DTO and enum it transitively references; auto-id fields become `ID`; enum fields auto-emit `enum` declarations; already-declared (manual or previously-derived) types are reused, not re-emitted.
  - `DslGraphQlSchema` gained `typeFrom(dto, name, description, exclude)`, `inputFrom(...)`, and `mapScalar(...)`.
  - `GraphQlDslCodegen` gained a `modelMappingMetadata` property; `GraphQlDslCodegenMain` now scans `ModelMappingDslCodegen` subclasses on the classpath, resolves their metadata, and injects it before running each GraphQL codegen.
  - `zygarde-graphql-codegen-dsl` now depends on `zygarde-model-mapping-codegen-dsl`.
- **Tests** — `GraphQlDtoDerivationTest` (9 tests): type/input derivation, ids/nullability/descriptions, transitive type + enum derivation, `exclude`, reuse of manually-declared types, end-to-end SDL rendering, and the two fail-fast paths (unmappable field type, DTO absent from metadata).
- **Sample** — `samples/todo-multimodule-dsl` `TodoGraphQlCodegen` now derives `Todo` / `TodoInput` via `typeFrom` / `inputFrom` (`TodoFilter` has no DTO and stays manual). `todo-codegen-dsl-graphql` gained dependencies on `todo-codegen-dsl-models` and `zygarde-model-mapping-codegen-dsl`. Regenerating changed only one line of `todoGraphQl.graphqls` — the auto-id `Todo.id` is now `ID!` instead of `Int!`; the generated controller / service interface are byte-identical, and `TodoGraphQlTest` / `BookGraphQlTest` still pass.
- `doc/graphql-dsl-guide.md` updated with a `typeFrom` / `inputFrom` section, the dependency notes, the module table, the limitations section, and the file index.

## Validation

- `./gradlew :zygarde-model-mapping-codegen-dsl:test :zygarde-graphql-codegen-dsl:test :zygarde-web-codegen:test :zygarde-graphql-codegen-dsl:ktlintCheck :zygarde-model-mapping-codegen-dsl:ktlintCheck` — all pass, including the 9 new derivation tests; existing model-mapping / GraphQL suites are unaffected.
- detekt is not wired to these modules' sources (prior rounds reported `NO-SOURCE`), so it was not re-run this round.

## Next Work

- Operation response derivation (`responseFrom(dto)`) so `query`/`mutation` return types do not need a hand-typed `graphQlType` string.
- Field-level customization on derived types (rename, extra computed fields, per-field deprecation) — currently `exclude` only; richer needs fall back to manual `type { }`.
- Consider GraphQL `interface` support (object types `implements` an interface) and interface/union `TypeResolver` wiring.
- Include super-interface properties of a `CodegenDtoWithSuperClass` in derivation (currently only declared field mappings are resolved).
- Decide whether generated schemas should remain split per DSL schema or be aggregated into one schema artifact.

## Blockers And Context

- No active blockers.
- `doc/graphql-dsl-guide.md` is the user-facing usage guide; `doc/graphql-support-investigation.md` remains the design/feasibility report and this file remains the round-to-round handoff.
- `typeFrom` / `inputFrom` take a model-mapping `CodegenDto` (the object declared in `ModelMappingCodegenSpec`), not the generated DTO class. Derivation needs the model-mapping codegen module on the GraphQL codegen classpath; `GraphQlDslCodegenMain` wires this automatically and unit tests may set `GraphQlDslCodegen.modelMappingMetadata` directly.
- `DtoMetaResolver` is the single source of truth for DTO field shape: the DTO class generator and the GraphQL deriver both go through it, so a derived GraphQL type always matches the generated DTO.
- `mapScalar` only registers a Kotlin-type → GraphQL-scalar-name mapping for derivation; it does not emit a `scalar` SDL declaration — call `scalar(...)` separately when the SDL needs one.
- The deriver fails fast when a DTO field type is neither a known/registered scalar, a Kotlin enum, nor a `dtoRef`, and when a requested DTO is absent from the model-mapping metadata.
- Auto-id fields (`fromAutoIntId` / `fromAutoLongId`) derive to the GraphQL built-in `ID`. Enum fields auto-emit a derived `enum`; a manually declared enum of the same name is reused instead.
- Schema-only GraphQL DSL declarations remain useful for shared SDL fragments such as scalars, unions, and common object/input types.
- `union` renders as `union Name = A | B` and, like `scalar`, produces no Kotlin controller/service artifacts; union/interface type resolution must still be wired manually via `RuntimeWiringConfigurer`.
- GraphQL runtime support is still sample/codegen focused; no dedicated `zygarde-graphql` runtime module exists yet.
- GraphQL `interface` types are not yet supported; error handling, authentication context injection, custom scalar registration, pagination shape, and generated DataLoader/batch resolver support remain open design and implementation areas.
