# zygarde-webmvc-codegen-ksp

A KSP (Kotlin Symbol Processing) based code generator for Spring WebMVC APIs. This module provides the same functionality as `zygarde-webmvc-codegen` but uses KSP instead of KAPT for better performance and Kotlin-native processing.

## Features

- **API Generation**: Generates API interfaces, Feign clients, controllers, and service interfaces from `@ZyApi` annotations
- **Static Option API**: Generates REST endpoints for enum options with `@StaticOptionApi`

## Usage

### Gradle Setup

```kotlin
plugins {
    id("com.google.devtools.ksp") version "1.9.25-1.0.20"
}

dependencies {
    ksp(project(":zygarde-webmvc-codegen-ksp"))
    implementation(project(":zygarde-webmvc"))
}

ksp {
    arg("zygarde.codegen.base.package", "com.example.generated")
    arg("zygarde.codegen.dto.package", "dto")
}

kotlin {
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
}
```

### API Definition

```kotlin
@ZyApi(
    group = "user",
    api = [
        GenApi(
            method = RequestMethod.GET,
            path = "/api/users",
            api = "UserApi.listUsers",
            apiDescription = "List all users",
            service = "UserService.listUsers",
            resRef = "UserDto",
            resCollection = true
        ),
        GenApi(
            method = RequestMethod.POST,
            path = "/api/users",
            api = "UserApi.createUser",
            apiDescription = "Create a user",
            service = "UserService.createUser",
            reqRef = "CreateUserReq",
            resRef = "UserDto"
        )
    ]
)
object UserApiSpec
```

### Static Option API

```kotlin
@StaticOptionApi(comment = "User Roles")
enum class UserRole(override val label: String) : OptionEnum {
    ADMIN("Administrator"),
    USER("Regular User")
}
```

### Generated Code

The processor generates:

1. **API Interface** - `UserApi.kt`
2. **Feign Client** - `UserApiFeign.kt`
3. **Controller** - `UserApiController.kt`
4. **Service Interface** - `UserService.kt`

For `@StaticOptionApi`:
1. **Static Option DTO** - `StaticOptionDto.kt`
2. **Static Option API** - `StaticOptionApi.kt`
3. **Static Option Controller** - `StaticOptionController.kt`

## KSP Options

| Option | Default | Description |
|--------|---------|-------------|
| `zygarde.codegen.base.package` | `zygarde.generated` | Base package for generated code |
| `zygarde.codegen.dto.package` | `data.dto` | Sub-package for DTOs |
| `zygarde.codegen.static.option.api.package` | `api.option` | Sub-package for static option API |

## Migration from KAPT

To migrate from the KAPT-based `zygarde-webmvc-codegen`:

1. Replace `kapt(project(":zygarde-webmvc-codegen"))` with `ksp(project(":zygarde-webmvc-codegen-ksp"))`
2. Add the KSP plugin to your build
3. Update the KSP source directory configuration
4. Note: JSON config file options are not supported in KSP version; use KSP args directly
