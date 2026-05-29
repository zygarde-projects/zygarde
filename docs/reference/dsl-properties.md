# DSL System Properties

Reference for configuring Zygarde's DSL-based code generation.

## Model Mapping DSL Properties

### zygarde.model.mapping.codegen.spec.class

**Description**: Fully qualified class name of the model mapping DSL specification

**Required**: Yes

**Example**:
```bash
-Dzygarde.model.mapping.codegen.spec.class=com.example.dsl.BookModelDsl
```

### zygarde.model.mapping.codegen.output.dir

**Description**: Output directory for generated model mapping code

**Required**: Yes

**Default**: `src/main/kotlin`

**Example**:
```bash
-Dzygarde.model.mapping.codegen.output.dir=src/main/kotlin
```

### Complete Example

```bash
./gradlew run \
  -Dzygarde.model.mapping.codegen.spec.class=com.example.dsl.BookModelDsl \
  -Dzygarde.model.mapping.codegen.output.dir=src/main/kotlin
```

## WebMVC DSL Properties

### zygarde.webmvc.codegen.spec.class

**Description**: Fully qualified class name of the WebMVC DSL specification

**Required**: Yes

**Example**:
```bash
-Dzygarde.webmvc.codegen.spec.class=com.example.dsl.BookApiDsl
```

### zygarde.webmvc.codegen.output.dir

**Description**: Output directory for generated web API code

**Required**: Yes

**Default**: `src/main/kotlin`

**Example**:
```bash
-Dzygarde.webmvc.codegen.output.dir=src/main/kotlin
```

### Complete Example

```bash
./gradlew run \
  -Dzygarde.webmvc.codegen.spec.class=com.example.dsl.BookApiDsl \
  -Dzygarde.webmvc.codegen.output.dir=src/main/kotlin
```

## Gradle Configuration

### Using gradle.properties

Add properties to `gradle.properties`:

```properties
# Model mapping
zygarde.model.mapping.codegen.spec.class=com.example.dsl.BookModelDsl
zygarde.model.mapping.codegen.output.dir=src/main/kotlin

# WebMVC
zygarde.webmvc.codegen.spec.class=com.example.dsl.BookApiDsl
zygarde.webmvc.codegen.output.dir=src/main/kotlin
```

### Using build.gradle.kts

```kotlin
tasks.register("generateModelMappings", JavaExec::class) {
  group = "code generation"
  description = "Generate model mappings from DSL"

  classpath = sourceSets["main"].runtimeClasspath
  mainClass.set("zygarde.codegen.dsl.model.ModelMappingCodegenKt")

  systemProperty("zygarde.model.mapping.codegen.spec.class", "com.example.dsl.BookModelDsl")
  systemProperty("zygarde.model.mapping.codegen.output.dir", "src/main/kotlin")
}

tasks.register("generateWebApi", JavaExec::class) {
  group = "code generation"
  description = "Generate web API from DSL"

  classpath = sourceSets["main"].runtimeClasspath
  mainClass.set("zygarde.codegen.dsl.webmvc.WebMvcCodegenKt")

  systemProperty("zygarde.webmvc.codegen.spec.class", "com.example.dsl.BookApiDsl")
  systemProperty("zygarde.webmvc.codegen.output.dir", "src/main/kotlin")
}
```

Run with:
```bash
./gradlew generateModelMappings
./gradlew generateWebApi
```

## SQL API DSL Properties

The SQL API DSL uses system properties on the codegen application module. The main class is:

```kotlin
zygarde.codegen.dsl.sqlapi.SqlApiDslCodegenMainKt
```

### Package Properties

| Property | Description | Default |
| --- | --- | --- |
| `zygarde.codegen.dsl.sql-api.dto.package` | Package for generated request and response DTOs | `zygarde.generated.dto` |
| `zygarde.codegen.dsl.sql-api.api-interface.package` | Package for generated API interfaces | `zygarde.generated.api` |
| `zygarde.codegen.dsl.sql-api.controller.package` | Package for generated controllers | `zygarde.generated.controller` |
| `zygarde.codegen.dsl.sql-api.service-interface.package` | Package for generated service interfaces | `zygarde.generated.service` |
| `zygarde.codegen.dsl.sql-api.service-impl.package` | Package for generated service implementations | `zygarde.generated.service.impl` |

### Output Directory Properties

If an output directory is not provided, generated files are written to stdout.

| Property | Description |
| --- | --- |
| `zygarde.codegen.dsl.sql-api.dto.write-to` | Output directory for generated DTOs |
| `zygarde.codegen.dsl.sql-api.api-interface.write-to` | Output directory for generated API interfaces |
| `zygarde.codegen.dsl.sql-api.feign-interface.write-to` | Output directory for generated Feign interfaces |
| `zygarde.codegen.dsl.sql-api.controller.write-to` | Output directory for generated controllers |
| `zygarde.codegen.dsl.sql-api.service-interface.write-to` | Output directory for generated service interfaces |
| `zygarde.codegen.dsl.sql-api.service-impl.write-to` | Output directory for generated service implementations |

### Complete Example

```kotlin
configure<JavaApplication> {
  mainClass.set("zygarde.codegen.dsl.sqlapi.SqlApiDslCodegenMainKt")
  applicationDefaultJvmArgs = listOf(
    "-Dzygarde.codegen.dsl.sql-api.dto.package=example.sqlapi.dto",
    "-Dzygarde.codegen.dsl.sql-api.api-interface.package=example.sqlapi.api",
    "-Dzygarde.codegen.dsl.sql-api.controller.package=example.sqlapi.controller",
    "-Dzygarde.codegen.dsl.sql-api.service-interface.package=example.sqlapi.service",
    "-Dzygarde.codegen.dsl.sql-api.service-impl.package=example.sqlapi.service.impl",
    "-Dzygarde.codegen.dsl.sql-api.dto.write-to=${project(":todo-dsl-generated-sql-api-dto").file("src/main/kotlin").absolutePath}",
    "-Dzygarde.codegen.dsl.sql-api.api-interface.write-to=${project(":todo-dsl-generated-sql-api-interface").file("src/main/kotlin").absolutePath}",
    "-Dzygarde.codegen.dsl.sql-api.feign-interface.write-to=${project(":todo-dsl-generated-sql-api-feign").file("src/main/kotlin").absolutePath}",
    "-Dzygarde.codegen.dsl.sql-api.controller.write-to=${project(":todo-dsl-generated-sql-api-controller").file("src/main/kotlin").absolutePath}",
    "-Dzygarde.codegen.dsl.sql-api.service-interface.write-to=${project(":todo-dsl-generated-sql-api-service-interface").file("src/main/kotlin").absolutePath}",
    "-Dzygarde.codegen.dsl.sql-api.service-impl.write-to=${project(":todo-dsl-generated-sql-api-service-impl").file("src/main/kotlin").absolutePath}",
  )
}
```

## Generation Scripts

### Shell Script

```bash
#!/bin/bash
# scripts/generate-code.sh

set -e

echo "Generating model mappings..."
./gradlew run \
  -Dzygarde.model.mapping.codegen.spec.class=com.example.dsl.BookModelDsl \
  -Dzygarde.model.mapping.codegen.output.dir=src/main/kotlin

echo "Generating web API..."
./gradlew run \
  -Dzygarde.webmvc.codegen.spec.class=com.example.dsl.BookApiDsl \
  -Dzygarde.webmvc.codegen.output.dir=src/main/kotlin

echo "Code generation complete!"
```

Make it executable:
```bash
chmod +x scripts/generate-code.sh
./scripts/generate-code.sh
```

## Multi-Module Projects

For multi-module projects, specify the module:

```bash
# Generate in codegen module
./gradlew :my-app-codegen:run \
  -Dzygarde.model.mapping.codegen.spec.class=com.example.dsl.BookModelDsl \
  -Dzygarde.model.mapping.codegen.output.dir=src/main/kotlin
```

## See Also

- [Model Mapping →](../guide/model-mapping.md) - Model mapping DSL guide
- [SQL API DSL →](../guide/sql-api.md) - SQL-backed API generation guide
- [Code Generation →](../guide/code-generation.md) - Code generation overview
- [DSL Tutorial →](../tutorials/dsl-based.md) - Complete DSL example
