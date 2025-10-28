# Quick Start

This quick start guide will walk you through creating a simple TODO application using Zygarde's KAPT-based code generation.

## Create Your First Entity

Define a JPA entity with the `@ZyModel` annotation:

```kotlin
package com.example.todo.model

import javax.persistence.*
import zygarde.codegen.apt.jpa.ZyModel

@Entity
@Table(name = "todos")
@ZyModel
data class Todo(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(nullable = false)
  val title: String,

  @Column(length = 1000)
  val description: String? = null,

  @Column(nullable = false)
  val completed: Boolean = false
)
```

## Build and Generate Code

Run the build to generate DAO and search DSL:

```bash
./gradlew build
```

Zygarde will generate:

1. **TodoDao** - Repository interface with CRUD operations
2. **Search DSL extensions** - Type-safe query methods

Generated files location: `build/generated/source/kapt/main/`

## Use the Generated DAO

Inject and use the generated DAO in your service:

```kotlin
package com.example.todo.service

import com.example.todo.codegen.Dao
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TodoService(private val dao: Dao) {

  fun getAllTodos() = dao.todo.findAll()

  fun getTodoById(id: Long) = dao.todo.findById(id)

  @Transactional
  fun createTodo(title: String, description: String?): Todo {
    val todo = Todo(title = title, description = description)
    return dao.todo.save(todo)
  }

  @Transactional
  fun completeTodo(id: Long): Todo? {
    val todo = dao.todo.findById(id).orElse(null) ?: return null
    val updated = todo.copy(completed = true)
    return dao.todo.save(updated)
  }
}
```

## Use the Search DSL

Query todos using the type-safe search DSL:

```kotlin
// Find incomplete todos
val incompleteTodos = dao.todo.search {
  completed() eq false
}

// Find todos by title (case-insensitive)
val kotlinTodos = dao.todo.search {
  title() contains "kotlin"
}

// Complex queries
val filteredTodos = dao.todo.search {
  and {
    completed() eq false
    title() notNull()
    description() like "%important%"
  }
}

// Enhanced DAO operations (if using ZygardeEnhancedDao)
dao.todo.remove {
  completed() eq true
}
```

## Create a REST Controller

Expose your service as a REST API:

```kotlin
package com.example.todo.controller

import com.example.todo.service.TodoService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/todos")
class TodoController(private val todoService: TodoService) {

  @GetMapping
  fun getAllTodos() = todoService.getAllTodos()

  @GetMapping("/{id}")
  fun getTodoById(@PathVariable id: Long) = todoService.getTodoById(id)

  @PostMapping
  fun createTodo(@RequestBody request: CreateTodoRequest) =
    todoService.createTodo(request.title, request.description)

  @PutMapping("/{id}/complete")
  fun completeTodo(@PathVariable id: Long) = todoService.completeTodo(id)
}

data class CreateTodoRequest(
  val title: String,
  val description: String?
)
```

## Application Configuration

Configure your Spring Boot application:

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    properties:
      hibernate:
        format_sql: true
```

## Run Your Application

```kotlin
package com.example.todo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TodoApplication

fun main(args: Array<String>) {
  runApplication<TodoApplication>(*args)
}
```

Start the application:

```bash
./gradlew bootRun
```

## Test the API

```bash
# Create a todo
curl -X POST http://localhost:8080/api/todos \
  -H "Content-Type: application/json" \
  -d '{"title":"Learn Zygarde","description":"Complete the quick start guide"}'

# Get all todos
curl http://localhost:8080/api/todos

# Complete a todo
curl -X PUT http://localhost:8080/api/todos/1/complete
```

## What You've Learned

- ✅ Annotate entities with `@ZyModel`
- ✅ Generate DAOs and search DSL automatically
- ✅ Use type-safe queries with the search DSL
- ✅ Inject and use generated DAOs
- ✅ Build a complete REST API

## Next Steps

- **[Project Setup →](project-setup.md)** - Configure a multi-module project
- **[JPA Extensions →](../guide/jpa-extensions.md)** - Learn advanced JPA features
- **[Search DSL →](../guide/search-dsl.md)** - Master the search DSL
- **[KAPT Tutorial →](../tutorials/kapt-based.md)** - Complete tutorial with more features

## Complete Example

For a complete working example, see the [todo-legacy sample](https://github.com/zygarde-projects/zygarde/tree/v2/samples/todo-legacy) in the Zygarde repository.
