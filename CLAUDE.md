# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Zygarde is a multi-module Kotlin framework for simplifying enterprise application development. It provides code generation (KAPT and DSL-based), JPA enhancements with type-safe search DSL, web/REST utilities, and model mapping capabilities for building Kotlin/Spring applications.

**Tech Stack**: Kotlin 1.8.22, Spring Boot 2.7.14, Gradle 7.x+ (Kotlin DSL), JUnit Platform, Kotest, MockK

## Essential Commands

### Build & Test
```bash
# Full build with tests and coverage
./gradlew build

# Run tests only
./gradlew test

# Test specific module
./gradlew -p modules-core/zygarde-core test

# Run sample application
./gradlew :samples:todo-legacy:bootRun
```

### Code Quality
```bash
# Auto-format code (do this before committing)
./gradlew ktlintFormat

# Check formatting
./gradlew ktlintCheck

# Run static analysis
./gradlew detekt

# Coverage is automatically reported after test runs
```

### Development
```bash
# Refresh dependency locks
./scripts/deps.sh

# Run single test class
./gradlew test --tests "ClassName"

# Run KAPT code generation
./gradlew kaptKotlin
```

## Module Architecture

### Module Structure
```
zygarde/
├── modules-core/           # Core utilities (error-handling, DI, Jackson, JWT, mail, etc.)
├── modules-jpa/            # JPA extensions, DAOs, search DSL, Envers support
├── modules-web/            # Web/REST utilities (WebMVC, WebFlux, security)
├── modules-model-mapping/  # Object mapping DSL and code generation
├── modules-codegen-support/# Code generation infrastructure and base classes
├── modules-test-support/   # Shared test fixtures and utilities
├── modules-bom/            # Bill of Materials for dependency management
├── samples/                # Example applications and generated code validation
└── doc/                    # Design documentation
```

**Convention**: Each module follows standard Kotlin layout:
- `src/main/kotlin/` - production code
- `src/test/kotlin/` - unit tests
- `build.gradle.kts` - module build config

**Pattern**: Codegen modules (`*-codegen`) contain annotation processors; runtime modules contain base classes/interfaces/implementations.

### Automatic Module Registration
Modules are auto-discovered in `settings.gradle.kts` via `registerModules()` function. Just add a directory with `build.gradle.kts` under `modules-*/` and it's included.

## Code Generation Architecture

### Two-Tier Code Generation System

#### 1. KAPT-Based (Annotation Processors)
Compile-time annotation processing for JPA entities:

**Key Annotation**: `@ZyModel` (place on `@Entity` classes)

**Generated Artifacts**:
- Entity field search extensions (DSL helper functions like `name()`, `author()`)
- DAO interfaces with configurable inheritance
- Combined `Dao` component aggregating all DAOs

**KAPT Configuration** (in `build.gradle.kts`):
```kotlin
kapt {
  arguments {
    arg("zygarde.codegen.base.package", "my.package.codegen")
    arg("zygarde.codegen.dao.inherit", "zygarde.data.jpa.dao.ZygardeEnhancedDao")
  }
}
```

**Key Options**:
- `zygarde.codegen.base.package` - base package for generated code
- `zygarde.codegen.dao.inherit` - custom base DAO class
- `zygarde.codegen.dao.suffix` - DAO naming suffix (default: "Dao")

#### 2. DSL-Based (Standalone Codegen)
Runtime DSL scripts that generate complete application layers:

**Model Mapping DSL** (`ModelMappingCodegenSpec`):
```kotlin
class MyModelDsl : ModelMappingCodegenSpec({
  MyDto {
    fromAutoIntId(Entity::id)      // Entity→DTO mapping
    from(Entity::description)
  }

  CreateMyReq {
    applyTo(Entity::description)   // DTO→Entity mapping
  }
})
```

**Web API DSL** (`WebMvcDslCodegen`):
Generates API interfaces, controllers, and service interfaces.

**Code Generation Workflow**:
1. Modify DSL definitions or annotation processors
2. Run code generation (KAPT or DSL script)
3. Regenerate sample outputs in `samples/todo-multimodule-dsl`
4. Review generated code diffs in version control
5. Ensure generated code compiles and passes tests

**IMPORTANT**: Never manually edit generated files. Keep them isolated in dedicated `*-codegen` modules or `doc/codegen-*` directories.

## JPA Extensions & Search DSL

### Type-Safe Search DSL Pattern

Zygarde provides a type-safe, fluent DSL for JPA queries that abstracts JPA Criteria API complexity:

```kotlin
// Generated DAO usage
bookDao.search {
  name() eq "MyBook"
  author().age() gt 30
  status() inList listOf(Status.ACTIVE, Status.PENDING)
}

// Enhanced DAO operations (if using ZygardeEnhancedDao)
bookDao.remove {
  name() eq "MyBook"
}
```

**Architecture**:
- `BaseDao<T, ID>` - combines `JpaRepository` + `JpaSpecificationExecutor`
- `EnhancedSearch<T>` - interface defining DSL operations
- `EnhancedSearchImpl<T>` - JPA Criteria implementation with predicate building
- Generated extension functions (e.g., `name()`, `author()`) return typed action objects

**Action Types**:
- `ConditionAction<Root, Current, Field>` - generic operations (eq, notEq, inList, isNull)
- `StringConditionAction` - string operations (contains, startsWith, like)
- `ComparableConditionAction` - numeric/comparable operations (gt, lt, gte, lte)

**Advanced Features**:
- Nested AND/OR predicates
- Automatic join handling for relationships
- Range overlap detection for temporal queries
- String concatenation in queries

### Entity Base Classes

**Generic ID Strategy**:
```kotlin
@MappedSuperclass
abstract class AutoIdEntity<T : Serializable>
  - AutoLongIdEntity (Long ID)
  - AutoIntIdEntity (Int ID)
  - SequenceIdEntity<T> (sequence-based ID)
```

**Audit Variants**: Available in `zygarde-jpa-envers` with Envers tracking.

**Pattern**: Uses generic type parameter for ID to support multiple ID types while maintaining type safety.

## Model Mapping Pattern

### Declarative DTO Mapping

The model mapping DSL provides three mapping directions:

1. **Model-to-DTO** (`from`/`fromAutoId`) - generates `.toXxxDto()` extensions
2. **DTO-to-Model** (`applyTo`) - generates `.applyFrom(dto)` extensions on entities
3. **Field-level** - with custom `ValueProvider` implementations

**Mapping Features**:
- Auto-mapping for ID fields (auto-increment variants)
- Reference/collection mapping to related DTOs
- Custom value transformations via `ValueProvider`
- Field extras and computed properties
- Inheritance support for DTO hierarchies

**Validation Integration**: The DSL supports javax.validation annotations (use `field` parameter in `applyTo` for validation target).

## Web/REST Utilities

### Three-Layer Generation Pattern

**Generated Artifacts**:
1. API Interface - contract definition with Spring annotations
2. Controller Implementation - HTTP endpoint routing
3. Service Interface - business logic contract
4. Feign Client (optional) - inter-service communication

### Exception Handling Strategy

- `ApiExceptionHandler` - Spring `@ControllerAdvice` with extensible mapper pattern
- `ExceptionToBusinessExceptionMapper<T>` - converts domain exceptions to `BusinessException`
- Supports validation errors, business exceptions, unknown exceptions
- Request tracing via `ApiTracingContext`

### API Response Standardization

- `PageDto<T>` - pagination wrapper (atPage, totalPages, items, totalCount)
- `ApiErrorResponse` - standardized error format
- Consistent HTTP status code handling

## Coding Standards

### Kotlin Style
- **Indentation**: 2 spaces (configured in `.editorconfig`)
- **Line Length**: 150 characters soft limit
- **Imports**: Explicit imports only; no wildcards
- **Naming**: camelCase for functions/properties, PascalCase for classes
- **Test Classes**: `<ClassName>Test` suffix

### Testing Standards

**Structure**: Use given/when/then pattern with Kotest assertions
```kotlin
@Test
fun `should create user with valid data`() {
  // given
  val userData = UserData(name = "John")

  // when
  val result = userService.createUser(userData)

  // then
  result shouldNotBe null
  result.name shouldBe "John"
}
```

**Guidelines**:
- Use Kotest assertions (`shouldBe`, `shouldNotBe`, `shouldThrow`)
- Use MockK for mocking (`mockk<T>()`, `every`, `verify`)
- Mirror production package structure in test directories
- Maintain or increase test coverage
- Name tests descriptively using backticks

### Package Naming
- Production packages: `zygarde.*` namespace
- Sample applications: own namespaces (e.g., `zygarde.samples.todo`)
- Generated code: dedicated `*-codegen` modules or `doc/codegen-*`

## Git Workflow

### Commit Message Format
Follow Conventional Commits: `<type>(<scope>): <subject>`

**Types**: `feat`, `fix`, `refactor`, `chore`, `docs`, `test`, `perf`, `style`, `ci`

**Examples**:
```
feat(jpa): add support for composite key entities
fix(web): correct JSON serialization for LocalDateTime
chore(deps): upgrade Spring Boot to 2.7.14
```

### Pre-Commit Checklist
1. Run `./gradlew ktlintFormat` to fix formatting
2. Run `./gradlew build` to ensure all tests pass
3. Run `./gradlew detekt` to verify static analysis
4. Check that coverage has not decreased
5. If touching code generation, regenerate samples and verify diffs

### Pull Request Guidelines
- **Title**: Use conventional commit format
- **Description**: Concise summary, reference issues, document migration steps
- **Verification**: All builds, ktlint, detekt, and tests pass locally

## Common Patterns & Best Practices

### Type Extension Pattern
- Extensions organized by concern in `/extension/` directories
- Examples: dates, strings, collections, UUID, SQL
- Use inline reified generics for type safety

### Common Data Structures
- **Pagination**: `PagingRequest`, `PagingAndSortingRequest`, `KeywordPagingAndSortingRequest`
- **Search Support**: `SearchKeyword`, `SearchDateRange`, `SearchRangeOverlap`
- **Options**: `OptionEnum`, `OptionDto` for select lists/enums

### Loggable Mixin
- SLF4J-based lazy logger via interface default method
- Use across handlers, services, and processors

### Extensibility Principles
1. **Composition over Configuration**: Extend base classes without modifying core generation logic
2. **Type Safety Through Generics**: Extensive use of reified generics and bounded type parameters
3. **DSL as Configuration**: Both KAPT options and DSL codegen use configuration-as-code
4. **Layered Isolation**: Generated code strictly separated from hand-written code
5. **Convention over Configuration**: Sensible defaults for naming while allowing overrides

## Publishing

**Artifacts published to**:
- Private Nexus repository (requires `PUNI_NEXUS_DEPLOY_USER`, `PUNI_NEXUS_DEPLOY_PWD`)
- GitHub Packages (requires `ZYGARDE_GH_USER`, `ZYGARDE_GH_TOKEN`)

**Scripts**:
- `./publish.sh` - Publish to Nexus
- `./publish-gh.sh` - Publish to GitHub Packages

## Troubleshooting

**Build fails with KAPT errors**:
- Run `./gradlew clean build`
- Check KAPT configuration in `build.gradle.kts`

**ktlint failures**:
- Run `./gradlew ktlintFormat` to auto-fix

**Test failures**:
- Run specific test: `./gradlew test --tests "ClassName.testMethod"`
- Check test isolation (no execution order dependencies)

**Coverage decreased**:
- Check JaCoCo HTML report in `build/reports/jacoco`
- Add missing test cases

## Key Architectural Insights

### Code Generation Philosophy
The framework uses **two complementary code generation approaches**:
1. **KAPT** for entity-driven generation (DAOs, search DSL)
2. **DSL scripts** for application-layer generation (mappings, controllers)

This separation allows entity-level metadata to be captured at compile-time while application-layer patterns remain flexible and can be regenerated independently.

### JPA Criteria Abstraction
The search DSL completely abstracts JPA Criteria API complexity through:
- **Type-safe field references** - generated extension functions ensure fields exist
- **Action objects** - fluent interface for operators avoids string-based queries
- **Automatic join resolution** - traversing relationships (e.g., `author().age()`) handled automatically

### Module Dependency Pattern
**Strict separation**:
- Codegen modules depend on runtime modules (for base classes, annotations)
- Runtime modules never depend on codegen modules
- Sample applications depend on both to validate generated code

This ensures the framework's runtime libraries remain lightweight and independent of code generation infrastructure.
