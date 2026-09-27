---
name: zygarde-codegen
description: Change Zygarde KAPT/KSP processors, codegen DSLs, generator configuration, or tracked generated samples. Use for generator behavior and sample regeneration; do not use for ordinary runtime-only changes.
---

# Zygarde code generation

Treat generator sources and DSL specs as the source of truth. Do not hand-edit files under `samples/todo-multimodule-dsl/todo-dsl-generated-*`.

## Locate the owning generator

- Shared processor support: `modules-codegen-support/`.
- JPA KAPT/KSP: `modules-jpa/zygarde-jpa-codegen*`.
- Model mapping KAPT/KSP/DSL: `modules-model-mapping/zygarde-model-mapping-codegen*`.
- WebMVC KAPT/KSP/DSL: `modules-web/zygarde-webmvc-codegen*`.
- GraphQL DSL: `modules-web/zygarde-graphql-codegen-dsl`.
- SQL API DSL: `modules-web/zygarde-sql-api-codegen-dsl`.

Follow an existing generator and test in the same family before introducing a new abstraction. Keep KAPT and KSP behavior aligned when they expose the same feature; if only one backend changes, state why.

## Implement and test

1. Change the generator or its DSL specification, not the generated sample.
2. Add or update the nearest generator test. Prefer asserting generated structure and compiling generated output over brittle whole-file text comparisons.
3. Run the focused module test while iterating, for example `./gradlew :zygarde-jpa-codegen:test`.
4. If the change affects the tracked DSL samples, regenerate them from the repository root in dependency order:

   ```bash
   ./gradlew :todo-codegen-dsl-apis:run
   ./gradlew :todo-codegen-dsl-graphql:run :todo-codegen-dsl-sql-api:run
   ```

   The API task already depends on the model-mapping generator.
5. Inspect `git diff -- samples/todo-multimodule-dsl`. Generated changes should be explainable by the source change; investigate unrelated churn or missing stale-file cleanup.
6. Finish with `$zygarde-verification`, including compilation/tests for affected sample consumers.

For KAPT or KSP integration behavior that is not represented by tracked DSL output, use the relevant sample compile task such as `:todo-legacy:kaptKotlin` or `:todo-ksp:kspKotlin`, then run that sample's tests when behavior reaches runtime.
