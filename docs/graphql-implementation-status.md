# GraphQL Implementation Status

Last updated: 2026-05-17

## Current State

- GraphQL feasibility and design notes live in `doc/graphql-support-investigation.md`.
- `modules-web/zygarde-web-codegen` contains the initial `GraphQlApiGenerator` and GraphQL generation value objects, including query, mutation, subscription, object type, input, enum, scalar, nullable collection, and raw SDL default-value support.
- `modules-web/zygarde-graphql-codegen-dsl` contains the first DSL entry point for query, mutation, subscription, type, input, enum, and scalar declarations, including Kotlin enum value derivation helpers and `GraphQlDefaultValue` helpers for safer SDL default-value literals.
- `samples/todo-multimodule-dsl` has a generated Todo GraphQL sample plus handwritten Book/Author GraphQL coverage for relation filtering and `@BatchMapping`.

## Changed In This Round

- Added the first user-facing GraphQL DSL usage guide at `doc/graphql-dsl-guide.md`, following the existing `doc/*-guide.md` convention.
- The guide documents the full DSL surface verified against the test suites: `GraphQlDslCodegen` entry point, `schema(...)` naming rules, query/mutation/subscription declarations, `argument`/`collectionArgument`, `returns`/`returnsCollection`, `type`/`input`/`enumType`/`scalar`, `defaultGraphQlType` Kotlin-to-SDL mapping, `GraphQlDefaultValue` literal helpers, `serviceName`/`serviceFunctionName` and the `DiServiceContext.bean<T>()` wiring, the `GraphQlDslCodegenMain` system properties, all fail-fast validation rules, and current limitations (nested resolvers, subscription publisher types, scalar coercing).
- No production code changed this round; this was a focused documentation round consolidating the seven prior validation/codegen rounds into usage docs.

## Validation

- `./gradlew :zygarde-web-codegen:test --tests zygarde.codegen.generator.GraphQlApiGeneratorTest :zygarde-graphql-codegen-dsl:test --tests zygarde.codegen.dsl.graphql.GraphQlDslCodegenTest` - passed (both suites green; documented behavior cross-checked against these tests).
- ktlint/detekt not run: the change is Markdown-only and not covered by those tasks.

## Next Work

- Link `doc/graphql-dsl-guide.md` from a top-level docs index or README if/when a docs index exists.
- Consider adding GraphQL SDL description (`"""..."""`) support across type definitions, fields, operation fields, and enum values; this is genuine missing schema behavior but touches many DSL overloads, so scope it deliberately.
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
- GraphQL runtime support is still sample/codegen focused; no dedicated `zygarde-graphql` runtime module exists yet.
- Error handling, authentication context injection, custom scalar registration, pagination shape, and generated DataLoader/batch resolver support remain open design and implementation areas.
