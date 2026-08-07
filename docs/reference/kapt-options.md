# KAPT Configuration Options

Complete reference for configuring Zygarde's KAPT annotation processor.

## Configuration

KAPT options are configured in `build.gradle.kts`:

```kotlin
kapt {
  arguments {
    arg("zygarde.codegen.base.package", "com.example.codegen")
    arg("zygarde.codegen.dao.inherit", "zygarde.data.jpa.dao.ZygardeEnhancedDao")
    arg("zygarde.codegen.dao.suffix", "Repository")
    arg("zygarde.codegen.dao.combine", "true")
  }
}
```

## Available Options

### zygarde.codegen.base.package

**Description**: Base package name for all generated files

**Type**: String

**Required**: Yes

**Default**: `zygarde.generated`

**Example**:
```kotlin
arg("zygarde.codegen.base.package", "com.example.myapp.codegen")
```

Generated structure:
```
com.example.myapp.codegen/
├── data/dao/          # DAOs
└── search/            # Search DSL extensions
```

### zygarde.codegen.dao.package

**Description**: Package for generated DAO files under base package

**Type**: String

**Required**: No

**Default**: `data.dao`

**Example**:
```kotlin
arg("zygarde.codegen.dao.package", "repository")
```

Result: `com.example.myapp.codegen.repository`

### zygarde.codegen.dao.suffix

**Description**: Suffix for generated DAO interface names

**Type**: String

**Required**: No

**Default**: `Dao`

**Example**:
```kotlin
// Default: BookDao
arg("zygarde.codegen.dao.suffix", "Dao")

// Custom: BookRepository
arg("zygarde.codegen.dao.suffix", "Repository")
```

### zygarde.codegen.dao.inherit

**Description**: Base interface or class for generated DAOs to extend

**Type**: Fully qualified class name

**Required**: No

**Default**: `org.springframework.data.jpa.repository.JpaRepository`

**Options**:
- `org.springframework.data.jpa.repository.JpaRepository` - Basic Spring Data JPA repository
- `zygarde.data.jpa.dao.BaseDao` - Zygarde base DAO with Specification support
- `zygarde.data.jpa.dao.ZygardeEnhancedDao` - Enhanced DAO with search DSL methods
- Custom class implementing your own base DAO

**Example**:
```kotlin
arg("zygarde.codegen.dao.inherit", "zygarde.data.jpa.dao.ZygardeEnhancedDao")
```

Generated code:
```kotlin
interface BookDao : ZygardeEnhancedDao<Book, Long>
```

### zygarde.codegen.dao.combine

**Description**: Generate an aggregated Dao component that injects all generated DAOs

**Type**: Boolean (true/false)

**Required**: No

**Default**: `true`

**Example**:
```kotlin
arg("zygarde.codegen.dao.combine", "true")
```

Generated code (when true):
```kotlin
@Component
class Dao {
  @Autowired
  lateinit var book: BookDao

  @Autowired
  lateinit var author: AuthorDao

  @Autowired
  lateinit var category: CategoryDao
}
```

Usage:
```kotlin
@Service
class BookService(private val dao: Dao) {
  fun getAllBooks() = dao.book.findAll()
  fun getAllAuthors() = dao.author.findAll()
}
```

## Complete Example

```kotlin
// build.gradle.kts
plugins {
  kotlin("kapt") version "1.8.22"
}

dependencies {
  implementation("zygarde:zygarde-jpa:VERSION")
  kapt("zygarde:zygarde-jpa-codegen:VERSION")
}

kapt {
  arguments {
    // Required: base package
    arg("zygarde.codegen.base.package", "com.example.bookstore.codegen")

    // Optional: customize DAO naming
    arg("zygarde.codegen.dao.package", "data.repository")
    arg("zygarde.codegen.dao.suffix", "Repository")

    // Optional: use enhanced DAO with search DSL
    arg("zygarde.codegen.dao.inherit", "zygarde.data.jpa.dao.ZygardeEnhancedDao")

    // Optional: enable aggregated Dao
    arg("zygarde.codegen.dao.combine", "true")
  }
}
```

Result:
```
com.example.bookstore.codegen/
└── data/repository/
    ├── BookRepository.kt
    ├── AuthorRepository.kt
    └── Dao.kt
```

## Custom Base DAO

Create your own base DAO:

```kotlin
package com.example.dao

import zygarde.data.jpa.dao.ZygardeEnhancedDao

interface CustomBaseDao<T, ID> : ZygardeEnhancedDao<T, ID> {

  // Add custom methods available to all DAOs
  fun findAllActive(): List<T>

  fun softDelete(id: ID)
}
```

Configure KAPT to use it:

```kotlin
kapt {
  arguments {
    arg("zygarde.codegen.dao.inherit", "com.example.dao.CustomBaseDao")
  }
}
```

Generated code will extend your custom base:

```kotlin
interface BookDao : CustomBaseDao<Book, Long>
```

## Verification

After configuration, run:

```bash
./gradlew kaptKotlin
```

Check generated files in:
```
build/generated/source/kapt/main/
└── [your.base.package]/
    └── data/dao/
        ├── [Entity][Suffix].kt
        └── Dao.kt  # if combine=true
```

## Common Issues

### Option Not Taking Effect

**Problem**: Changes to KAPT options don't seem to apply

**Solution**:
```bash
./gradlew clean kaptKotlin
```

### Generated Code Package Mismatch

**Problem**: Generated code appears in wrong package

**Solution**: Verify `zygarde.codegen.base.package` is set correctly and rebuild

### Custom Base DAO Not Found

**Problem**: Compilation error about missing custom base DAO

**Solution**: Ensure custom base DAO is compiled before KAPT runs (usually in a separate module)

## See Also

- [Code Generation →](../guide/code-generation.md) - Overview of KAPT generation
- [JPA Extensions →](../guide/jpa-extensions.md) - Using generated DAOs
- [KAPT Tutorial →](../tutorials/kapt-based.md) - Step-by-step guide
