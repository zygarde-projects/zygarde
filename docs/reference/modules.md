# Module Reference

Complete reference of all Zygarde modules and their purposes.

## Core Modules (`modules-core/`)

### zygarde-core

**Purpose**: Core utilities and common functionality

**Key Features**:
- Exception handling (`BusinessException`, exception mappers)
- Common DTOs (`PagingRequest`, `SearchKeyword`, `PageDto`)
- Extension functions (dates, strings, collections, UUID)
- Option/enum utilities

**Maven Coordinates**:
```kotlin
implementation("zygarde:zygarde-core:VERSION")
```

### zygarde-jackson

**Purpose**: Jackson JSON serialization configuration

**Key Features**:
- Custom serializers/deserializers
- Java 8 date/time support
- Kotlin module configuration

**Maven Coordinates**:
```kotlin
implementation("zygarde:zygarde-jackson:VERSION")
```

### zygarde-jwt

**Purpose**: JWT authentication utilities

**Key Features**:
- JWT token generation and validation
- Spring Security integration helpers

**Maven Coordinates**:
```kotlin
implementation("zygarde:zygarde-jwt:VERSION")
```

### zygarde-mail

**Purpose**: Email sending utilities

**Key Features**:
- Template-based email sending
- Spring Mail integration

**Maven Coordinates**:
```kotlin
implementation("zygarde:zygarde-mail:VERSION")
```

### zygarde-codegen-support

**Purpose**: Base classes for code generation infrastructure

**Key Features**:
- Code generation utilities
- Template helpers
- AST manipulation tools

**Maven Coordinates**:
```kotlin
implementation("zygarde:zygarde-codegen-support:VERSION")
```

## JPA Modules (`modules-jpa/`)

### zygarde-jpa

**Purpose**: JPA extensions and enhanced DAO support

**Key Features**:
- Entity base classes (`AutoLongIdEntity`, `AutoIntIdEntity`)
- Enhanced DAO interfaces (`BaseDao`, `ZygardeEnhancedDao`)
- Type-safe search DSL runtime
- Search action implementations

**Maven Coordinates**:
```kotlin
implementation("zygarde:zygarde-jpa:VERSION")
```

### zygarde-jpa-codegen

**Purpose**: KAPT annotation processor for generating DAOs and search DSL

**Key Features**:
- Processes `@ZyModel` annotations
- Generates DAO interfaces
- Generates search DSL extension functions
- Generates aggregated Dao component

**Maven Coordinates**:
```kotlin
kapt("zygarde:zygarde-jpa-codegen:VERSION")
```

### zygarde-jpa-envers

**Purpose**: Hibernate Envers integration for entity auditing

**Key Features**:
- Audit entity base classes (`AuditAutoLongIdEntity`)
- Created/modified tracking
- User tracking integration

**Maven Coordinates**:
```kotlin
implementation("zygarde:zygarde-jpa-envers:VERSION")
```

## Web Modules (`modules-web/`)

### zygarde-webmvc

**Purpose**: Spring WebMVC utilities and REST support

**Key Features**:
- Exception handling (`ApiExceptionHandler`)
- Request tracing (`ApiTracingContext`)
- CORS configuration helpers
- Response standardization utilities

**Maven Coordinates**:
```kotlin
implementation("zygarde:zygarde-webmvc:VERSION")
```

### zygarde-webflux

**Purpose**: Spring WebFlux reactive utilities

**Key Features**:
- Reactive exception handling
- Reactive request tracing
- WebFlux-specific utilities

**Maven Coordinates**:
```kotlin
implementation("zygarde:zygarde-webflux:VERSION")
```

### zygarde-webmvc-codegen-dsl

**Purpose**: DSL for generating WebMVC controllers and APIs

**Key Features**:
- API interface generation
- Controller implementation generation
- Service interface generation
- Declarative endpoint definition

**Maven Coordinates**:
```kotlin
implementation("zygarde:zygarde-webmvc-codegen-dsl:VERSION")
```

## Model Mapping Modules (`modules-model-mapping/`)

### zygarde-model-mapping-core

**Purpose**: Core model mapping abstractions

**Key Features**:
- Value provider interfaces
- Mapping strategy definitions
- Field mapping utilities

**Maven Coordinates**:
```kotlin
implementation("zygarde:zygarde-model-mapping-core:VERSION")
```

### zygarde-model-mapping-codegen-dsl

**Purpose**: DSL for generating DTO mappings

**Key Features**:
- Declarative mapping definitions
- DTO generation
- Mapping function generation
- Validation annotation support

**Maven Coordinates**:
```kotlin
implementation("zygarde:zygarde-model-mapping-codegen-dsl:VERSION")
```

## Supporting Modules

### zygarde-bom-codegen

**Purpose**: Bill of Materials for code generation dependencies

**Usage**:
```kotlin
dependencies {
  implementation(platform("zygarde:zygarde-bom-codegen:VERSION"))
}
```

### zygarde-bom-codegen-test

**Purpose**: Bill of Materials for code generation testing

**Usage**:
```kotlin
dependencies {
  testImplementation(platform("zygarde:zygarde-bom-codegen-test:VERSION"))
}
```

### modules-test-support

**Purpose**: Shared test fixtures and utilities

**Key Features**:
- Common test helpers
- Mock data builders
- Test base classes

## Dependency Graph

```
Application Layer
    ↓
┌───────────────────────────────────┐
│  zygarde-webmvc                   │
│  zygarde-model-mapping-core       │
└───────────────┬───────────────────┘
                ↓
┌───────────────────────────────────┐
│  zygarde-jpa                      │
└───────────────┬───────────────────┘
                ↓
┌───────────────────────────────────┐
│  zygarde-core                     │
│  zygarde-jackson                  │
└───────────────────────────────────┘

Code Generation (compile-time only)
┌───────────────────────────────────┐
│  zygarde-jpa-codegen              │
│  zygarde-model-mapping-codegen    │
│  zygarde-webmvc-codegen-dsl       │
└───────────────────────────────────┘
```

## Module Selection Guide

### For Basic JPA Projects
```kotlin
dependencies {
  implementation("zygarde:zygarde-jpa:VERSION")
  kapt("zygarde:zygarde-jpa-codegen:VERSION")
}
```

### For REST API Projects
```kotlin
dependencies {
  implementation("zygarde:zygarde-jpa:VERSION")
  implementation("zygarde:zygarde-webmvc:VERSION")
  kapt("zygarde:zygarde-jpa-codegen:VERSION")
}
```

### For Full-Stack Projects with DSL
```kotlin
dependencies {
  implementation("zygarde:zygarde-jpa:VERSION")
  implementation("zygarde:zygarde-webmvc:VERSION")
  implementation("zygarde:zygarde-model-mapping-core:VERSION")
  implementation("zygarde:zygarde-model-mapping-codegen-dsl:VERSION")
  implementation("zygarde:zygarde-webmvc-codegen-dsl:VERSION")
  kapt("zygarde:zygarde-jpa-codegen:VERSION")
}
```

## Version Compatibility

| Zygarde Version | Kotlin | Spring Boot | Java |
|-----------------|--------|-------------|------|
| 1.x.x          | 1.8.22 | 2.7.14      | 8+   |

## See Also

- [Architecture →](../guide/architecture.md) - Module architecture details
- [Getting Started →](../getting-started/index.md) - Installation guide
