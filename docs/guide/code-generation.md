# Code Generation

Zygarde provides two complementary approaches to code generation, each serving different purposes in your application architecture.

## Overview

### Two-Tier Code Generation System

1. **KAPT-Based**: Compile-time annotation processing for entity-driven code
2. **DSL-Based**: Runtime script generation for application layer code

This separation allows entity-level metadata to be captured at compile-time while keeping application-layer patterns flexible and independently regenerable.

## KAPT-Based Generation

### Purpose

Generate DAOs and type-safe search DSL from annotated JPA entities at compile-time.

### Key Annotation

Place `@ZyModel` on your `@Entity` classes:

```kotlin
import javax.persistence.*
import zygarde.codegen.apt.jpa.ZyModel

@Entity
@Table(name = "books")
@ZyModel
data class Book(
  @Id @GeneratedValue
  val id: Long? = null,

  @Column(nullable = false)
  val title: String,

  @ManyToOne(fetch = FetchType.LAZY)
  val author: Author
)
```

### Generated Artifacts

When you build your project, Zygarde generates:

#### 1. Entity-Specific DAO Interfaces

```kotlin
// Generated: BookDao.kt
interface BookDao : ZygardeEnhancedDao<Book, Long>
```

#### 2. Search DSL Extension Functions

```kotlin
// Generated: BookSearchDsl.kt
fun SearchScope<Book, Book>.title(): StringConditionAction<Book, Book, String>
fun SearchScope<Book, Book>.author(): ConditionAction<Book, Book, Author>
```

#### 3. Aggregated Dao Component

```kotlin
// Generated: Dao.kt
@Component
class Dao(
  val book: BookDao,
  val author: AuthorDao,
  // ... all other DAOs
)
```

### Configuration

Configure KAPT in your `build.gradle.kts`:

```kotlin
plugins {
  kotlin("kapt") version "1.8.22"
}

dependencies {
  implementation("zygarde:zygarde-jpa:VERSION")
  kapt("zygarde:zygarde-jpa-codegen:VERSION")
}

kapt {
  arguments {
    // Required: base package for generated code
    arg("zygarde.codegen.base.package", "com.example.myapp.codegen")

    // Optional: custom base DAO class
    arg("zygarde.codegen.dao.inherit", "zygarde.data.jpa.dao.ZygardeEnhancedDao")

    // Optional: DAO naming suffix (default: "Dao")
    arg("zygarde.codegen.dao.suffix", "Repository")
  }
}
```

### KAPT Configuration Options

| Option | Description | Default | Required |
|--------|-------------|---------|----------|
| `zygarde.codegen.base.package` | Base package for generated code | - | Yes |
| `zygarde.codegen.dao.inherit` | Base DAO interface/class | `org.springframework.data.jpa.repository.JpaRepository` | No |
| `zygarde.codegen.dao.suffix` | Suffix for DAO class names | `Dao` | No |

### Custom Base DAO

Create a custom base DAO to add shared functionality:

```kotlin
interface MyCustomDao<T, ID> : ZygardeEnhancedDao<T, ID> {
  // Add custom methods available to all DAOs
  fun findAllActive(): List<T> {
    return findAll().filter { it.isActive }
  }
}
```

Configure KAPT to use it:

```kotlin
kapt {
  arguments {
    arg("zygarde.codegen.dao.inherit", "com.example.dao.MyCustomDao")
  }
}
```

### Build Process

```bash
# Generate code and compile
./gradlew build

# Only run code generation
./gradlew kaptKotlin

# Clean and regenerate
./gradlew clean kaptKotlin
```

Generated code location: `build/generated/source/kapt/main/`

## DSL-Based Generation

### Purpose

Generate complete application layers including model mappings, REST controllers, and service interfaces using declarative DSL scripts.

### Model Mapping DSL

#### Defining Mappings

Create a DSL specification class:

```kotlin
import zygarde.codegen.dsl.model.ModelMappingCodegenSpec

class TodoModelDsl : ModelMappingCodegenSpec({

  // DTO class definition
  TodoDto {
    // Entity → DTO mapping
    fromAutoLongId(Todo::id)
    from(Todo::title)
    from(Todo::description)
    from(Todo::completed)
    from(Todo::createdAt)
  }

  // Create request DTO
  CreateTodoReq {
    // DTO → Entity mapping
    applyTo(Todo::title)
    applyTo(Todo::description)
    // With validation
    applyTo(Todo::priority, field = "priority") {
      validation("@Min(1)", "@Max(5)")
    }
  }

  // Update request DTO
  UpdateTodoReq {
    applyTo(Todo::title)
    applyTo(Todo::description)
    applyTo(Todo::completed)
  }
})
```

#### Mapping Directions

**1. Model-to-DTO** (`from`, `fromAutoId`):

Generates extension functions on entities:

```kotlin
// Generated
fun Todo.toTodoDto() = TodoDto(
  id = this.id,
  title = this.title,
  description = this.description,
  completed = this.completed,
  createdAt = this.createdAt
)
```

**2. DTO-to-Model** (`applyTo`):

Generates extension functions on DTOs:

```kotlin
// Generated
fun Todo.applyFrom(dto: CreateTodoReq): Todo {
  return this.copy(
    title = dto.title,
    description = dto.description,
    priority = dto.priority
  )
}
```

**3. Reference Mapping**:

Map related entities to DTOs:

```kotlin
TodoDto {
  fromAutoLongId(Todo::id)
  from(Todo::title)
  // Map author relationship
  fromRef(Todo::author, "authorDto") { author ->
    author.toAuthorDto()
  }
}
```

#### Validation Integration

Use `field` parameter with validation annotations:

```kotlin
CreateTodoReq {
  applyTo(Todo::title, field = "title") {
    validation(
      "@NotBlank",
      "@Size(min = 1, max = 200)"
    )
  }
  applyTo(Todo::description, field = "description") {
    validation("@Size(max = 1000)")
  }
}
```

#### Running Model Mapping Generation

```bash
# Set system properties
-Dzygarde.model.mapping.codegen.spec.class=com.example.TodoModelDsl
-Dzygarde.model.mapping.codegen.output.dir=src/main/kotlin

# Run generation
./gradlew run -PmainClass=zygarde.codegen.dsl.model.ModelMappingCodegenKt
```

### Web API DSL

#### Defining REST APIs

Create a DSL specification for REST endpoints:

```kotlin
import zygarde.codegen.dsl.webmvc.WebMvcDslCodegen

class TodoApiDsl : WebMvcDslCodegen({
  api("TodoApi") {
    basePath = "/api/todos"

    endpoint {
      name = "getAllTodos"
      method = GET
      path = ""
      returns = "List<TodoDto>"
    }

    endpoint {
      name = "getTodoById"
      method = GET
      path = "/{id}"
      pathParams = listOf("id" to "Long")
      returns = "TodoDto"
    }

    endpoint {
      name = "createTodo"
      method = POST
      path = ""
      requestBody = "CreateTodoReq"
      returns = "TodoDto"
    }

    endpoint {
      name = "updateTodo"
      method = PUT
      path = "/{id}"
      pathParams = listOf("id" to "Long")
      requestBody = "UpdateTodoReq"
      returns = "TodoDto"
    }

    endpoint {
      name = "deleteTodo"
      method = DELETE
      path = "/{id}"
      pathParams = listOf("id" to "Long")
      returns = "Unit"
    }
  }
})
```

#### Generated Artifacts

**1. API Interface**:

```kotlin
@RequestMapping("/api/todos")
interface TodoApi {

  @GetMapping
  fun getAllTodos(): List<TodoDto>

  @GetMapping("/{id}")
  fun getTodoById(@PathVariable id: Long): TodoDto

  @PostMapping
  fun createTodo(@RequestBody request: CreateTodoReq): TodoDto

  @PutMapping("/{id}")
  fun updateTodo(@PathVariable id: Long, @RequestBody request: UpdateTodoReq): TodoDto

  @DeleteMapping("/{id}")
  fun deleteTodo(@PathVariable id: Long)
}
```

**2. Controller Implementation**:

```kotlin
@RestController
class TodoController(
  private val todoService: TodoService
) : TodoApi {

  override fun getAllTodos() = todoService.getAllTodos()

  override fun getTodoById(id: Long) = todoService.getTodoById(id)

  override fun createTodo(request: CreateTodoReq) = todoService.createTodo(request)

  override fun updateTodo(id: Long, request: UpdateTodoReq) =
    todoService.updateTodo(id, request)

  override fun deleteTodo(id: Long) = todoService.deleteTodo(id)
}
```

**3. Service Interface**:

```kotlin
interface TodoService {
  fun getAllTodos(): List<TodoDto>
  fun getTodoById(id: Long): TodoDto
  fun createTodo(request: CreateTodoReq): TodoDto
  fun updateTodo(id: Long, request: UpdateTodoReq): TodoDto
  fun deleteTodo(id: Long)
}
```

#### Running Web API Generation

```bash
-Dzygarde.webmvc.codegen.spec.class=com.example.TodoApiDsl
-Dzygarde.webmvc.codegen.output.dir=src/main/kotlin

./gradlew run -PmainClass=zygarde.codegen.dsl.webmvc.WebMvcCodegenKt
```

## Code Generation Workflow

### Development Workflow

1. **Modify source** (entities, DSL specs)
2. **Run generation** (KAPT or DSL script)
3. **Review generated code**
4. **Compile and test**
5. **Commit both source and generated code** (if not gitignored)

### Multi-Module Workflow

For projects with separate codegen modules:

```bash
# Generate code in domain module
./gradlew :my-app-domain:kaptKotlin

# Compile codegen module (depends on domain)
./gradlew :my-app-codegen:compileKotlin

# Build entire project
./gradlew build
```

### Regeneration

#### KAPT Regeneration

```bash
# Clean generated code
./gradlew clean

# Regenerate
./gradlew kaptKotlin build
```

#### DSL Regeneration

Simply rerun the DSL generation script. Generated files will be overwritten.

## Zygarde Code Generation Practices

### 1. Never Manually Edit Generated Code

Zygarde-generated files are build artifacts. Make changes via:
- Entity annotations (`@ZyModel`)
- DSL specifications
- KAPT configuration options

### 2. Use Separate Modules for Generated Code

```
my-app/
├── my-app-domain/       # Source entities
├── my-app-codegen/      # Zygarde-generated code
└── my-app-service/      # Uses generated code
```

### 3. Version Control Strategy for Generated Code

**Option A**: Gitignore generated code (regenerate in CI)
```gitignore
build/generated/
*-codegen/src/
```

**Option B**: Commit generated code
- Easier to review Zygarde output changes
- Debugging is simpler
- No generation step in CI

### 4. Document Zygarde Generation Process

Add README in codegen modules:

```markdown
# Zygarde Generated Code Module

**DO NOT EDIT FILES IN THIS MODULE DIRECTLY**

To regenerate:
1. Modify entities in `my-app-domain`
2. Run `./gradlew :my-app-domain:kaptKotlin`
3. Zygarde will regenerate this module
```

## Troubleshooting

### KAPT Not Generating Code

**Symptoms**: No generated files in `build/generated/`

**Solutions**:
1. Verify `@ZyModel` annotation present on entities
2. Check KAPT plugin is applied: `kotlin("kapt")`
3. Verify KAPT configuration matches Zygarde requirements
4. Run `./gradlew clean build --info`

### Generated Code Not Compiling

**Symptoms**: Compilation errors in generated code

**Solutions**:
1. Check entity relationships are properly annotated
2. Verify custom base DAO exists if configured
3. Ensure all referenced types are on classpath
4. Review KAPT processor logs

### DSL Generation Fails

**Symptoms**: DSL script throws exceptions

**Solutions**:
1. Verify DSL syntax is correct
2. Check all referenced entity types exist
3. Ensure system properties are set correctly
4. Review stack trace for specific errors

## Next Steps

- **[JPA Extensions →](jpa-extensions.md)** - Use generated DAOs effectively
- **[Model Mapping →](model-mapping.md)** - Advanced mapping patterns
- **[KAPT Tutorial →](../tutorials/kapt-based.md)** - Complete KAPT example
- **[DSL Tutorial →](../tutorials/dsl-based.md)** - Complete DSL example
- **[KAPT Options Reference →](../reference/kapt-options.md)** - All configuration options
