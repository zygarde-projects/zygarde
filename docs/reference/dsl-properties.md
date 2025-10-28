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
- [Code Generation →](../guide/code-generation.md) - Code generation overview
- [DSL Tutorial →](../tutorials/dsl-based.md) - Complete DSL example
