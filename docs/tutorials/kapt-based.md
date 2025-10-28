# KAPT-Based Tutorial

This tutorial demonstrates building a complete TODO application using Zygarde's KAPT-based code generation approach.

## Overview

You'll learn to:
- Set up KAPT code generation
- Define entities with `@ZyModel`
- Use generated DAOs and search DSL
- Build a REST API with generated code

## Prerequisites

- JDK 8 or higher
- Basic knowledge of Kotlin and Spring Boot
- Completed [Quick Start](../getting-started/quick-start.md)

## Complete Sample Project

The full working example is available at:
**[samples/todo-legacy](https://github.com/zygarde-projects/zygarde/tree/v2/samples/todo-legacy)**

## Step 1: Project Setup

### Dependencies

Add Zygarde JPA dependencies:

```kotlin
// build.gradle.kts
plugins {
  kotlin("jvm") version "1.8.22"
  kotlin("kapt") version "1.8.22"
  kotlin("plugin.spring") version "1.8.22"
  id("org.springframework.boot") version "2.7.14"
}

dependencies {
  // Zygarde JPA with code generation
  implementation("zygarde:zygarde-jpa:VERSION")
  kapt("zygarde:zygarde-jpa-codegen:VERSION")

  // Spring Boot
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-web")

  // Database
  runtimeOnly("com.h2database:h2")
}

kapt {
  arguments {
    arg("zygarde.codegen.base.package", "com.example.todo.codegen")
    arg("zygarde.codegen.dao.inherit", "zygarde.data.jpa.dao.ZygardeEnhancedDao")
  }
}
```

## Step 2: Define Entities

Create your domain entities with `@ZyModel`:

```kotlin
package com.example.todo.model

import javax.persistence.*
import zygarde.codegen.apt.jpa.ZyModel
import zygarde.data.jpa.entity.AutoIntIdEntity

@Entity
@Table(name = "todos")
@ZyModel
class Todo(
  @Column(nullable = false, length = 200)
  var title: String = "",

  @Column(length = 1000)
  var description: String = "",

  @Column(nullable = false)
  var completed: Boolean = false,

  @Column(nullable = false)
  var priority: Int = 3
) : AutoIntIdEntity()
```

## Step 3: Generate Code

Run KAPT to generate DAOs and search DSL:

```bash
./gradlew kaptKotlin
```

Generated files will be in: `build/generated/source/kapt/main/`

Generated artifacts:
- `TodoDao.kt` - Repository interface
- `TodoSearchDsl.kt` - Search extension functions
- `Dao.kt` - Aggregated DAO component

## Step 4: Create DTOs

Define request and response DTOs:

```kotlin
// Response DTO
data class TodoDto(
  val id: Int?,
  val title: String,
  val description: String,
  val completed: Boolean,
  val priority: Int
)

// Create request DTO
data class CreateTodoReq(
  @field:NotBlank
  @field:Size(min = 1, max = 200)
  val title: String,

  @field:Size(max = 1000)
  val description: String = "",

  @field:Min(1)
  @field:Max(5)
  val priority: Int = 3
)

// Update request DTO
data class UpdateTodoReq(
  @field:Size(min = 1, max = 200)
  val title: String?,

  @field:Size(max = 1000)
  val description: String?,

  val completed: Boolean?,

  @field:Min(1)
  @field:Max(5)
  val priority: Int?
)
```

## Step 5: Implement Service

Use generated DAOs in your service:

```kotlin
@Service
class TodoService(private val dao: Dao) {

  @Transactional(readOnly = true)
  fun getAllTodos(): List<TodoDto> {
    return dao.todo.findAll().map { it.toDto() }
  }

  @Transactional(readOnly = true)
  fun getTodoById(id: Int): TodoDto {
    return dao.todo.findById(id)
      .orElseThrow { NoSuchElementException("Todo not found: $id") }
      .toDto()
  }

  @Transactional
  fun createTodo(request: CreateTodoReq): TodoDto {
    val todo = Todo(
      title = request.title,
      description = request.description,
      priority = request.priority
    )
    return dao.todo.save(todo).toDto()
  }

  @Transactional
  fun updateTodo(id: Int, request: UpdateTodoReq): TodoDto {
    val todo = dao.todo.findById(id)
      .orElseThrow { NoSuchElementException("Todo not found: $id") }

    request.title?.let { todo.title = it }
    request.description?.let { todo.description = it }
    request.completed?.let { todo.completed = it }
    request.priority?.let { todo.priority = it }

    return dao.todo.save(todo).toDto()
  }

  @Transactional
  fun deleteTodo(id: Int) {
    dao.todo.deleteById(id)
  }

  @Transactional(readOnly = true)
  fun searchTodos(keyword: String?): List<TodoDto> {
    if (keyword.isNullOrBlank()) return getAllTodos()

    return dao.todo.search {
      or {
        title() containsIgnoreCase keyword
        description() containsIgnoreCase keyword
      }
    }.map { it.toDto() }
  }
}

// Extension function for DTO mapping
private fun Todo.toDto() = TodoDto(
  id = this.id,
  title = this.title,
  description = this.description,
  completed = this.completed,
  priority = this.priority
)
```

## Step 6: Create REST Controller

Expose your service as REST API:

```kotlin
@RestController
@RequestMapping("/api/todos")
class TodoController(private val todoService: TodoService) {

  @GetMapping
  fun getAllTodos(@RequestParam(required = false) search: String?): List<TodoDto> {
    return if (search != null) {
      todoService.searchTodos(search)
    } else {
      todoService.getAllTodos()
    }
  }

  @GetMapping("/{id}")
  fun getTodoById(@PathVariable id: Int): TodoDto {
    return todoService.getTodoById(id)
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  fun createTodo(@Valid @RequestBody request: CreateTodoReq): TodoDto {
    return todoService.createTodo(request)
  }

  @PutMapping("/{id}")
  fun updateTodo(
    @PathVariable id: Int,
    @Valid @RequestBody request: UpdateTodoReq
  ): TodoDto {
    return todoService.updateTodo(id, request)
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun deleteTodo(@PathVariable id: Int) {
    todoService.deleteTodo(id)
  }
}
```

## Step 7: Configure Application

### Application Configuration

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:h2:mem:todo
    driver-class-name: org.h2.Driver
  h2:
    console:
      enabled: true
      path: /h2-console
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    properties:
      hibernate:
        format_sql: true
```

### Main Application Class

```kotlin
@SpringBootApplication
class TodoApplication

fun main(args: Array<String>) {
  runApplication<TodoApplication>(*args)
}
```

## Step 8: Run and Test

### Start the Application

```bash
./gradlew bootRun
```

### Test the API

```bash
# Create a todo
curl -X POST http://localhost:8080/api/todos \
  -H "Content-Type: application/json" \
  -d '{"title":"Learn Zygarde","description":"Complete KAPT tutorial","priority":5}'

# Get all todos
curl http://localhost:8080/api/todos

# Search todos
curl http://localhost:8080/api/todos?search=Zygarde

# Update a todo
curl -X PUT http://localhost:8080/api/todos/1 \
  -H "Content-Type: application/json" \
  -d '{"completed":true}'

# Delete a todo
curl -X DELETE http://localhost:8080/api/todos/1
```

## Using the Search DSL

The generated search DSL provides powerful querying capabilities:

```kotlin
// Find incomplete todos
dao.todo.search {
  completed() eq false
}

// Find high-priority incomplete todos
dao.todo.search {
  completed() eq false
  priority() gte 4
}

// Complex search with OR conditions
dao.todo.search {
  completed() eq false
  or {
    priority() gte 4
    title() contains "urgent"
  }
}

// Bulk operations
val deletedCount = dao.todo.remove {
  completed() eq true
}
```

## What You've Learned

✅ Setting up KAPT code generation
✅ Annotating entities with `@ZyModel`
✅ Using generated DAOs and search DSL
✅ Building a complete REST API
✅ Implementing CRUD operations
✅ Using type-safe queries

## Next Steps

- **[DSL-Based Tutorial →](dsl-based.md)** - Learn DSL code generation
- **[Search DSL →](../guide/search-dsl.md)** - Master advanced queries
- **[JPA Extensions →](../guide/jpa-extensions.md)** - Explore more JPA features
- **[Complete Sample →](https://github.com/zygarde-projects/zygarde/tree/v2/samples/todo-legacy)** - Full source code

## Troubleshooting

### Generated Code Not Found

If generated code is not visible:
1. Run `./gradlew clean kaptKotlin`
2. Refresh IDE (IntelliJ: File → Invalidate Caches)
3. Verify KAPT configuration

### Compilation Errors

Check that:
- KAPT plugin is applied
- `@ZyModel` annotation is present
- Entity extends appropriate base class
- KAPT configuration includes `zygarde.codegen.base.package`
