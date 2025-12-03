# zygarde-jpa-codegen-ksp

A KSP (Kotlin Symbol Processing) based code generator for JPA entities. This module provides the same functionality as `zygarde-jpa-codegen` but uses KSP instead of KAPT for better performance and Kotlin-native processing.

## Features

- **DAO Generation**: Automatically generates JPA repository interfaces for entities annotated with `@ZyModel` and `@Entity`
- **Search Extensions**: Generates type-safe extension functions for enhanced entity searching
- **Combined Dao Class**: Optionally generates a combined `Dao` class containing all generated DAOs

## Usage

### Gradle Setup

```kotlin
plugins {
    id("com.google.devtools.ksp") version "1.9.25-1.0.20"
}

dependencies {
    ksp(project(":zygarde-jpa-codegen-ksp"))
    implementation(project(":zygarde-jpa"))
}

ksp {
    arg("zygarde.codegen.base.package", "com.example.generated")
    arg("zygarde.codegen.dao.package", "dao")
    arg("zygarde.codegen.entity.search", "search")
}

kotlin {
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
}
```

### Entity Definition

```kotlin
@ZyModel
@Entity
class Todo(
    var description: String = "",
    var completed: Boolean = false
) : AutoIntIdEntity()
```

### Generated Code

The processor generates:

1. **TodoDao.kt** - JPA Repository interface
```kotlin
interface TodoDao : JpaRepository<Todo, Int>, JpaSpecificationExecutor<Todo>
```

2. **TodoExtensions.kt** - Search extension functions
```kotlin
fun EnhancedSearch<Todo>.description(): StringConditionAction<Todo, Todo>
fun EnhancedSearch<Todo>.completed(): ComparableConditionAction<Todo, Todo, Boolean>
```

3. **Dao.kt** - Combined DAO class (optional)
```kotlin
@Component
class Dao(@Autowired val todoDao: TodoDao)
```

## KSP Options

| Option | Default | Description |
|--------|---------|-------------|
| `zygarde.codegen.base.package` | `zygarde.generated` | Base package for generated code |
| `zygarde.codegen.dao.package` | `data.dao` | Sub-package for DAO interfaces |
| `zygarde.codegen.entity.search` | `entity.search` | Sub-package for search extensions |
| `zygarde.codegen.dao.suffix` | `Dao` | Suffix for generated DAO names |
| `zygarde.codegen.dao.inherit` | - | Custom base interface for DAOs |
| `zygarde.codegen.dao.combine` | `true` | Generate combined Dao class |

## Migration from KAPT

To migrate from the KAPT-based `zygarde-jpa-codegen`:

1. Replace `kapt(project(":zygarde-jpa-codegen"))` with `ksp(project(":zygarde-jpa-codegen-ksp"))`
2. Add the KSP plugin to your build
3. Update the KSP source directory configuration
4. KSP options use the same keys as KAPT options
