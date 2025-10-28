# Architecture

This guide explains Zygarde's module architecture, design principles, and how the components work together.

## Module Structure

Zygarde is organized into focused modules, each serving a specific purpose:

```
zygarde/
├── modules-core/           # Core utilities
├── modules-jpa/            # JPA extensions, DAOs, search DSL
├── modules-web/            # Web/REST utilities
├── modules-model-mapping/  # Object mapping DSL
├── modules-codegen-support/# Code generation infrastructure
├── modules-test-support/   # Shared test fixtures
├── modules-bom/            # Bill of Materials
├── samples/                # Example applications
└── doc/                    # Design documentation
```

### Module Categories

#### Core Modules (`modules-core/`)

Foundation utilities used across the framework:

- **zygarde-core**: Error handling, dependency injection helpers, common extensions
- **zygarde-jackson**: JSON serialization configuration
- **zygarde-jwt**: JWT authentication utilities
- **zygarde-mail**: Email sending utilities
- **zygarde-codegen-support**: Base classes for code generation

#### JPA Modules (`modules-jpa/`)

JPA enhancements and type-safe querying:

- **zygarde-jpa**: Base DAOs, entity base classes, search DSL runtime
- **zygarde-jpa-codegen**: KAPT annotation processor for generating DAOs
- **zygarde-jpa-envers**: Audit entity base classes with Envers

#### Web Modules (`modules-web/`)

Web and REST utilities:

- **zygarde-webmvc**: Spring WebMVC utilities, exception handlers
- **zygarde-webflux**: Spring WebFlux utilities
- **zygarde-webmvc-codegen-dsl**: DSL for generating controllers

#### Model Mapping Modules (`modules-model-mapping/`)

Object mapping and DTO generation:

- **zygarde-model-mapping-core**: Core mapping abstractions
- **zygarde-model-mapping-codegen-dsl**: DSL for generating model mappings

#### Supporting Modules

- **modules-bom/**: Gradle Bill of Materials for version management
- **modules-test-support/**: Shared test utilities and fixtures
- **samples/**: Working example applications

## Module Conventions

### Standard Layout

Each module follows a consistent structure:

```
module-name/
├── build.gradle.kts        # Module build configuration
├── src/
│   ├── main/
│   │   └── kotlin/         # Production code
│   │       └── zygarde/    # Package structure
│   └── test/
│       └── kotlin/         # Test code
└── README.md              # Module documentation (optional)
```

### Codegen Module Pattern

Modules with code generation capabilities follow this pattern:

- **Runtime module** (`*-runtime` or base name): Contains base classes, interfaces, and runtime support
- **Codegen module** (`*-codegen`): Contains annotation processors or DSL code generators

Example: `zygarde-jpa` (runtime) + `zygarde-jpa-codegen` (KAPT processor)

### Automatic Module Registration

Modules are automatically discovered by `settings.gradle.kts` through the `registerModules()` function:

```kotlin
fun registerModules(prefix: String) {
  file(prefix).listFiles()?.forEach { dir ->
    if (dir.isDirectory && file("${dir.path}/build.gradle.kts").exists()) {
      include(":${dir.name}")
      project(":${dir.name}").projectDir = dir
    }
  }
}

registerModules("modules-core")
registerModules("modules-jpa")
registerModules("modules-web")
// ...
```

**Adding a new module**: Simply create a directory under `modules-*/` with a `build.gradle.kts` file.

## Dependency Relationships

### Core Dependency Graph

```
┌─────────────────────────────────────────────────────┐
│                   Application                       │
└──────────────┬──────────────────────────────────────┘
               │
       ┌───────┴─────────┬─────────────────┐
       │                 │                 │
┌──────▼──────┐  ┌───────▼────────┐  ┌────▼──────┐
│ zygarde-web │  │ zygarde-jpa    │  │ Model     │
│             │  │                │  │ Mapping   │
└──────┬──────┘  └────────┬───────┘  └─────┬─────┘
       │                  │                 │
       │         ┌────────▼────────┐        │
       │         │ zygarde-core    │        │
       └─────────►                 ◄────────┘
                 └─────────────────┘
```

### Module Dependency Rules

1. **Core modules** have no dependencies on other Zygarde modules
2. **JPA modules** depend only on core modules
3. **Web modules** may depend on core and JPA modules
4. **Codegen modules** depend on their corresponding runtime modules
5. **Runtime modules** never depend on codegen modules

This ensures:
- Clean separation of concerns
- No circular dependencies
- Lightweight runtime libraries

## Code Generation Architecture

Zygarde employs a two-tier code generation strategy:

### 1. KAPT-Based Generation

**Purpose**: Generate DAOs and search DSL from annotated entities

**Workflow**:
```
@Entity + @ZyModel → KAPT Processor → Generated DAOs + Search Extensions
```

**Key Components**:
- `@ZyModel` annotation marks entities for generation
- KAPT processor in `zygarde-jpa-codegen`
- Generated code: DAOs, search DSL methods, aggregated `Dao` class

**Configuration**: Via KAPT arguments in `build.gradle.kts`

### 2. DSL-Based Generation

**Purpose**: Generate application layer code (mappings, controllers, services)

**Workflow**:
```
DSL Definition → Code Generator → Generated Code Files
```

**Key Components**:
- `ModelMappingCodegenSpec` for model mappings
- `WebMvcDslCodegen` for REST controllers
- Standalone code generation scripts

**Configuration**: Via system properties and DSL definitions

### Generation Isolation

Generated code is strictly isolated from hand-written code:

```
project/
├── src/main/kotlin/           # Hand-written code
└── build/generated/source/
    └── kapt/main/             # Generated code (gitignored)
```

For multi-module projects:
```
my-app/
├── my-app-domain/             # Entities with @ZyModel
├── my-app-codegen/            # Generated code target
└── my-app-service/            # Uses generated code
```

## Design Principles

### 1. Composition Over Configuration

Extend base classes without modifying core generation logic:

```kotlin
// Define custom base DAO
abstract class MyEnhancedDao<T, ID> : ZygardeEnhancedDao<T, ID> {
  // Add custom methods
}

// Configure KAPT to use it
kapt {
  arguments {
    arg("zygarde.codegen.dao.inherit", "com.example.MyEnhancedDao")
  }
}
```

### 2. Type Safety Through Generics

Extensive use of reified generics and bounded type parameters:

```kotlin
interface EnhancedSearch<T : Any> {
  fun search(block: SearchScope<T, T>.() -> Unit): List<T>
}
```

### 3. DSL as Configuration

Both KAPT options and DSL codegen use configuration-as-code:

```kotlin
// KAPT configuration
kapt {
  arguments {
    arg("zygarde.codegen.base.package", "com.example.codegen")
  }
}

// DSL configuration
MyDto {
  fromAutoIntId(Entity::id)
  from(Entity::description)
}
```

### 4. Layered Isolation

Clear boundaries between layers:

- **Framework layer**: Core utilities, base classes
- **Generated layer**: KAPT/DSL generated code
- **Application layer**: Business logic using generated code

### 5. Convention Over Configuration

Sensible defaults with override capability:

- Default DAO suffix: `Dao` (configurable via `zygarde.codegen.dao.suffix`)
- Default base DAO: `JpaRepository` (configurable via `zygarde.codegen.dao.inherit`)
- Default search DSL operators based on field types

## Extension Points

### Custom Search Actions

Extend search DSL with custom actions:

```kotlin
class CustomAction<ROOT, CURRENT, FIELD>(
  /* ... */
) : ConditionAction<ROOT, CURRENT, FIELD>(/* ... */) {

  infix fun customOperator(value: FIELD): ROOT {
    // Custom predicate logic
    return root
  }
}
```

### Custom Value Providers

Create custom value transformation logic:

```kotlin
class CustomValueProvider<FROM, TO> : ValueProvider<FROM, TO> {
  override fun invoke(from: FROM): TO {
    // Transformation logic
  }
}
```

### Custom Exception Mappers

Handle domain-specific exceptions:

```kotlin
@Component
class MyExceptionMapper : ExceptionToBusinessExceptionMapper<MyException> {
  override val exceptionClass = MyException::class.java

  override fun map(exception: MyException) = BusinessException(
    message = exception.message,
    code = "MY_ERROR"
  )
}
```

## Architectural Insights

### Why Two Code Generation Approaches?

1. **KAPT for entity-driven generation**: Captures entity metadata at compile-time
2. **DSL for application-layer generation**: Flexible, regenerable independently

This separation allows:
- Entity metadata captured without runtime overhead
- Application patterns remain flexible
- Independent regeneration cycles

### JPA Criteria Abstraction

The search DSL abstracts JPA Criteria complexity:

- **Type-safe field references**: Generated extensions ensure fields exist
- **Action objects**: Fluent interface avoids string-based queries
- **Automatic join resolution**: Relationship traversal handled automatically

Example of automatic join resolution:
```kotlin
dao.book.search {
  author().country() eq "USA"  // Automatically creates join
}
```

### Module Dependency Pattern

**Strict separation ensures**:
- Codegen modules depend on runtime modules (for base classes)
- Runtime modules never depend on codegen modules
- Framework remains lightweight without generation infrastructure

## Next Steps

- **[Code Generation →](code-generation.md)** - Learn both generation approaches
- **[JPA Extensions →](jpa-extensions.md)** - Explore JPA features
- **[Common Patterns →](../reference/common-patterns.md)** - Best practices
