# zygarde-model-mapping-codegen-ksp

A KSP (Kotlin Symbol Processing) based code generator for model mapping. This module provides the same functionality as `zygarde-model-mapping-codegen` but uses KSP instead of KAPT for better performance and Kotlin-native processing.

## Features

- **DTO Generation**: Automatically generates DTOs from entities annotated with `@ZyModel` and `@ApiProp`
- **Extension Functions**: Generates `toXxxDto()` and `applyFromXxxReq()` extension functions
- **Search Extensions**: Generates type-safe search extensions for request DTOs with `SearchType`

## Usage

### Gradle Setup

```kotlin
plugins {
    id("com.google.devtools.ksp") version "1.9.25-1.0.20"
}

dependencies {
    ksp(project(":zygarde-model-mapping-codegen-ksp"))
    implementation(project(":zygarde-model-mapping"))
}

ksp {
    arg("zygarde.codegen.base.package", "com.example.generated")
    arg("zygarde.codegen.dto.package", "dto")
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
class User(
    @ApiProp(
        comment = "user name",
        dto = [Dto("UserDto")],
        requestDto = [RequestDto("CreateUserReq")]
    )
    var name: String = "",
    
    @ApiProp(
        comment = "user email",
        dto = [Dto("UserDto")],
        requestDto = [
            RequestDto("CreateUserReq"),
            RequestDto("UpdateUserReq", forceNullableInReq = true)
        ]
    )
    var email: String = ""
) : AutoIntIdEntity()
```

### Generated Code

The processor generates:

1. **DTOs**
```kotlin
@Schema
data class UserDto(
    @Schema(description = "user name", requiredMode = Schema.RequiredMode.REQUIRED)
    var name: String,
    @Schema(description = "user email", requiredMode = Schema.RequiredMode.REQUIRED)
    var email: String
) : Serializable
```

2. **Extension Functions**
```kotlin
fun User.toUserDto(): UserDto

fun User.applyFromCreateUserReq(req: CreateUserReq): User
```

## KSP Options

| Option | Default | Description |
|--------|---------|-------------|
| `zygarde.codegen.base.package` | `zygarde.generated` | Base package for generated code |
| `zygarde.codegen.dto.package` | `data.dto` | Sub-package for DTOs |
| `zygarde.codegen.entity.search` | `entity.search` | Sub-package for search extensions |

## Migration from KAPT

To migrate from the KAPT-based `zygarde-model-mapping-codegen`:

1. Replace `kapt(project(":zygarde-model-mapping-codegen"))` with `ksp(project(":zygarde-model-mapping-codegen-ksp"))`
2. Add the KSP plugin to your build
3. Update the KSP source directory configuration
4. KSP options use the same keys as KAPT options
