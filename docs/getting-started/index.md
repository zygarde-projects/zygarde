# Getting Started with Zygarde

This guide shows you how to add Zygarde to an existing Kotlin/Spring Boot project and configure code generation.

## Prerequisites

- Existing Spring Boot 2.7+ project with Kotlin
- Gradle 7.x+ with Kotlin DSL
- JDK 8+ installed

## Add Zygarde Dependencies

### Core Dependencies

The minimum setup requires only two dependencies:

```kotlin
// build.gradle.kts
plugins {
  kotlin("kapt") version "1.8.22"  // Required for code generation
  // ... your existing plugins
}

repositories {
  maven("https://nexus.puni.tw/repository/maven-releases")
  // OR GitHub Packages
  maven {
    url = uri("https://maven.pkg.github.com/zygarde-projects/zygarde")
    credentials {
      username = System.getenv("GITHUB_ACTOR")
      password = System.getenv("GITHUB_TOKEN")
    }
  }
}

dependencies {
  // These two dependencies are all you need to start
  implementation("zygarde:zygarde-jpa:VERSION")
  kapt("zygarde:zygarde-jpa-codegen:VERSION")
}
```

### KAPT Configuration (Required)

Configure where Zygarde generates code:

```kotlin
kapt {
  arguments {
    // Required: where to generate DAO code
    arg("zygarde.codegen.base.package", "com.example.myapp.codegen")

    // Optional: use enhanced DAO with search() method
    arg("zygarde.codegen.dao.inherit", "zygarde.data.jpa.dao.ZygardeEnhancedDao")
  }
}
```

**Key Options:**

| Option | What it Does | Required |
|--------|--------------|----------|
| `zygarde.codegen.base.package` | Package for generated code | ✅ Yes |
| `zygarde.codegen.dao.inherit` | Base DAO interface | No (defaults to Spring Data's JpaRepository) |

See [KAPT Options Reference](../reference/kapt-options.md) for all options.

## Verify Setup

### 1. Annotate an Entity

```kotlin
@Entity
@ZyModel  // ← Zygarde annotation triggers code generation
data class Book(
  @Id @GeneratedValue
  val id: Long? = null,
  val title: String
)
```

### 2. Build Project

```bash
./gradlew kaptKotlin
```

### 3. Check Generated Code

Look in `build/generated/source/kapt/main/[your.package]/`:
- `BookDao.kt` - Repository interface
- `Dao.kt` - Aggregated DAO component
- Search DSL extensions

## Next Steps

Now that Zygarde is installed:

1. **[Quick Start →](quick-start.md)** - Build your first Zygarde application
2. **[Project Setup →](project-setup.md)** - Configure a complete project
3. **[Architecture →](../guide/architecture.md)** - Understand Zygarde's structure

## Common Issues

### KAPT Not Generating Code

**Problem**: No code is generated after build.

**Solution**: Ensure:
1. KAPT plugin is applied: `kotlin("kapt")`
2. Entity is annotated with `@ZyModel`
3. KAPT configuration includes required options
4. Clean build: `./gradlew clean build`

### Repository Not Found

**Problem**: Cannot resolve Zygarde dependencies.

**Solution**: Verify repository configuration and credentials for GitHub Packages or Nexus.

### Version Compatibility

**Problem**: Dependency conflicts or incompatible versions.

**Solution**: Check the [compatibility matrix](../reference/modules.md) and ensure consistent versions across all Zygarde modules.
