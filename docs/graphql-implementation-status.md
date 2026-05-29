# GraphQL Implementation Status

Last updated: 2026-05-30 (generated provider-backed GraphQL batch resolver context hook)

## Current State

- GraphQL feasibility and design notes live in `doc/graphql-support-investigation.md`.
- `modules-web/zygarde-web-codegen` contains the `GraphQlApiGenerator` and GraphQL generation value objects, including query, mutation, subscription, object type, input, enum, scalar, union, nullable collection, raw SDL default-value, SDL descriptions, and `@deprecated` directives on every GraphQL position.
- `modules-web/zygarde-graphql-codegen-dsl` contains the DSL entry point for query, mutation, subscription, type, input, enum, scalar, and union declarations, plus **`typeFrom` / `inputFrom` derivation of `type` / `input` declarations directly from model-mapping DTO metadata** and `lazyProviders` support for provider-backed generated `@BatchMapping` methods.
- `modules-model-mapping/zygarde-model-mapping-codegen-dsl` now exposes `DtoMetaResolver` / `ModelMappingMetadata` / `ResolvedDtoField` — the single source of truth for a DTO's resolved field shape, shared by the DTO class generator and the GraphQL deriver.
- `samples/todo-multimodule-dsl` has a generated Todo GraphQL sample, including a provider-backed generated `Todo.file` `@BatchMapping`, plus handwritten Book/Author GraphQL coverage for relation filtering.

## Changed In This Round

Generated provider-backed `@BatchMapping` methods now resolve `DataProviderContext` through an optional `DataProviderContextResolver` bean before calling `DataProvider.load(...)`. If no resolver bean exists, generated code falls back to `DataProviderContext.EMPTY`.

- **Core hook** — `zygarde.data.provider.DataProviderContextResolver` exposes `resolve(): DataProviderContext`.
- **Generated GraphQL controller** — provider-backed batch mappings now look up `DataProviderContextResolver` with `DiServiceContext.ctx.getBeanProvider(...).ifAvailable`, use the resolved context when present, and retain the previous `EMPTY` behavior when absent.
- **Sample** — `Todo.file` remains generated from `lazyProviders`; `TodoGraphQlTest` now verifies that the generated resolver calls `FileDtoProvider` with a test context marker.
- **Docs** — `doc/graphql-dsl-guide.md` now describes `lazyProviders` batch resolver generation and clarifies that GraphQL Java `DataLoaderRegistry` support is still not generated.

## Validation

- `./gradlew :zygarde-core:test :zygarde-web-codegen:test :zygarde-graphql-codegen-dsl:test` — passes.
- `./gradlew :todo-codegen-dsl-graphql:run` — passes and synchronizes the generated sample GraphQL controller.
- `./gradlew :todo-src-main:test` — passes. The sample project is included at root as `:todo-src-main`, not under a `:samples` Gradle project path.
- `./gradlew :zygarde-core:ktlintCheck :zygarde-web-codegen:ktlintCheck :zygarde-graphql-codegen-dsl:ktlintCheck :todo-src-core:ktlintCheck :todo-src-main:ktlintCheck :todo-dsl-generated-graphql-controller:ktlintCheck` — passes.

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
- GraphQL `interface` types are not yet supported. Provider-backed generated `@BatchMapping` is supported through `lazyProviders`, but GraphQL Java `DataLoaderRegistry` generation is not.
