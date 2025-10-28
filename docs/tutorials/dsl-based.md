# DSL-Based Tutorial

This tutorial demonstrates building a TODO application using Zygarde's DSL-based code generation for model mappings and web APIs.

## Overview

You'll learn to:
- Define model mapping DSL for DTOs
- Generate web API layers with DSL
- Build a multi-module project with generated code
- Use DSL for rapid application development

## Prerequisites

- Completed [KAPT-Based Tutorial](kapt-based.md)
- Understanding of multi-module Gradle projects
- Familiarity with Kotlin DSL syntax

## Complete Sample Project

The full working example is available at:
**[samples/todo-multimodule-dsl](https://github.com/zygarde-projects/zygarde/tree/v2/samples/todo-multimodule-dsl)**

## Project Structure

Multi-module project with separation of concerns:

```
todo-app/
├── todo-domain/          # Entities with @ZyModel
├── todo-codegen/         # DSL definitions and generated code
├── todo-service/         # Business logic
└── todo-web/             # REST controllers and main app
```

## Step 1: Domain Module

### Entity Definition

```kotlin
// todo-domain/src/main/kotlin/model/Todo.kt
package com.example.todo.model

import javax.persistence.*
import zygarde.codegen.apt.jpa.ZyModel
import zygarde.data.jpa.entity.AutoLongIdEntity

@Entity
@Table(name = "todos")
@ZyModel
class Todo(
  @Column(nullable = false)
  var title: String = "",

  @Column(length = 1000)
  var description: String? = null,

  @Column(nullable = false)
  var completed: Boolean = false,

  @Column(nullable = false)
  var priority: Int = 3,

  @Column(nullable = false)
  var createdAt: LocalDateTime = LocalDateTime.now()
) : AutoLongIdEntity()
```

### Build Configuration

```kotlin
// todo-domain/build.gradle.kts
plugins {
  kotlin("jvm")
  kotlin("kapt")
}

dependencies {
  api("zygarde:zygarde-jpa:VERSION")
  kapt("zygarde:zygarde-jpa-codegen:VERSION")
}

kapt {
  arguments {
    arg("zygarde.codegen.base.package", "com.example.todo.codegen")
  }
}
```

## Step 2: Model Mapping DSL

### Define DTO Mappings

```kotlin
// todo-codegen/src/main/kotlin/dsl/TodoModelDsl.kt
package com.example.todo.codegen.dsl

import zygarde.codegen.dsl.model.ModelMappingCodegenSpec
import com.example.todo.model.Todo

class TodoModelDsl : ModelMappingCodegenSpec({

  // Response DTO: Entity → DTO
  TodoDto {
    fromAutoLongId(Todo::id)
    from(Todo::title)
    from(Todo::description)
    from(Todo::completed)
    from(Todo::priority)
    from(Todo::createdAt)
  }

  // Create Request: DTO → Entity
  CreateTodoReq {
    applyTo(Todo::title, field = "title") {
      validation(
        "@NotBlank(message = \"Title is required\")",
        "@Size(min = 1, max = 200)"
      )
    }

    applyTo(Todo::description, field = "description") {
      validation("@Size(max = 1000)")
    }

    applyTo(Todo::priority, field = "priority") {
      validation(
        "@Min(1)",
        "@Max(5)"
      )
    }
  }

  // Update Request: DTO → Entity
  UpdateTodoReq {
    applyTo(Todo::title, field = "title") {
      validation("@Size(min = 1, max = 200)")
    }

    applyTo(Todo::description, field = "description") {
      validation("@Size(max = 1000)")
    }

    applyTo(Todo::completed, field = "completed")

    applyTo(Todo::priority, field = "priority") {
      validation(
        "@Min(1)",
        "@Max(5)"
      )
    }
  }
})
```

### Generate Model Mappings

Run the DSL code generator:

```bash
./gradlew -p todo-codegen run \
  -Dzygarde.model.mapping.codegen.spec.class=com.example.todo.codegen.dsl.TodoModelDsl \
  -Dzygarde.model.mapping.codegen.output.dir=src/main/kotlin
```

Generated files:
- `TodoDto.kt` - Response DTO with `.toTodoDto()` extension
- `CreateTodoReq.kt` - Create request DTO
- `UpdateTodoReq.kt` - Update request DTO
- `TodoMappings.kt` - Mapping extension functions

## Step 3: Web API DSL

### Define API Contract

```kotlin
// todo-codegen/src/main/kotlin/dsl/TodoApiDsl.kt
package com.example.todo.codegen.dsl

import zygarde.codegen.dsl.webmvc.WebMvcDslCodegen
import zygarde.codegen.dsl.webmvc.HttpMethod.*

class TodoApiDsl : WebMvcDslCodegen({

  api("TodoApi") {
    basePath = "/api/todos"
    packageName = "com.example.todo.api"

    endpoint {
      name = "getAllTodos"
      method = GET
      path = ""
      returns = "List<TodoDto>"
      params = listOf("search" to "String?")
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
      validate = true
    }

    endpoint {
      name = "updateTodo"
      method = PUT
      path = "/{id}"
      pathParams = listOf("id" to "Long")
      requestBody = "UpdateTodoReq"
      returns = "TodoDto"
      validate = true
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

### Generate Web Layer

```bash
./gradlew -p todo-codegen run \
  -Dzygarde.webmvc.codegen.spec.class=com.example.todo.codegen.dsl.TodoApiDsl \
  -Dzygarde.webmvc.codegen.output.dir=src/main/kotlin
```

Generated files:
- `TodoApi.kt` - API interface with Spring annotations
- `TodoController.kt` - Controller implementation
- `TodoService.kt` - Service interface

## Step 4: Service Implementation

Implement the generated service interface:

```kotlin
// todo-service/src/main/kotlin/service/TodoServiceImpl.kt
package com.example.todo.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.example.todo.codegen.Dao

@Service
class TodoServiceImpl(private val dao: Dao) : TodoService {

  @Transactional(readOnly = true)
  override fun getAllTodos(search: String?): List<TodoDto> {
    return if (search.isNullOrBlank()) {
      dao.todo.findAll().map { it.toTodoDto() }
    } else {
      dao.todo.search {
        or {
          title() containsIgnoreCase search
          description() containsIgnoreCase search
        }
      }.map { it.toTodoDto() }
    }
  }

  @Transactional(readOnly = true)
  override fun getTodoById(id: Long): TodoDto {
    return dao.todo.findById(id)
      .orElseThrow { NoSuchElementException("Todo not found: $id") }
      .toTodoDto()
  }

  @Transactional
  override fun createTodo(request: CreateTodoReq): TodoDto {
    val todo = Todo().applyFrom(request)
    return dao.todo.save(todo).toTodoDto()
  }

  @Transactional
  override fun updateTodo(id: Long, request: UpdateTodoReq): TodoDto {
    val todo = dao.todo.findById(id)
      .orElseThrow { NoSuchElementException("Todo not found: $id") }

    val updated = todo.applyFrom(request)
    return dao.todo.save(updated).toTodoDto()
  }

  @Transactional
  override fun deleteTodo(id: Long) {
    dao.todo.deleteById(id)
  }
}
```

## Step 5: Wire Everything Together

### Main Application

```kotlin
// todo-web/src/main/kotlin/Application.kt
package com.example.todo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(
  scanBasePackages = [
    "com.example.todo",
    "com.example.todo.codegen"
  ]
)
class TodoApplication

fun main(args: Array<String>) {
  runApplication<TodoApplication>(*args)
}
```

### Configuration

```yaml
# todo-web/src/main/resources/application.yml
spring:
  datasource:
    url: jdbc:h2:mem:todo
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true

server:
  port: 8080
```

## Step 6: Build and Run

### Build All Modules

```bash
# Generate KAPT code
./gradlew :todo-domain:kaptKotlin

# Generate DSL code (model mappings)
./gradlew :todo-codegen:run \
  -Dzygarde.model.mapping.codegen.spec.class=com.example.todo.codegen.dsl.TodoModelDsl

# Generate DSL code (web API)
./gradlew :todo-codegen:run \
  -Dzygarde.webmvc.codegen.spec.class=com.example.todo.codegen.dsl.TodoApiDsl

# Build entire project
./gradlew build
```

### Run Application

```bash
./gradlew :todo-web:bootRun
```

### Test the API

```bash
# Create todo
curl -X POST http://localhost:8080/api/todos \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Learn DSL Generation",
    "description": "Master Zygarde DSL-based approach",
    "priority": 5
  }'

# Get all todos
curl http://localhost:8080/api/todos

# Search todos
curl "http://localhost:8080/api/todos?search=DSL"

# Update todo
curl -X PUT http://localhost:8080/api/todos/1 \
  -H "Content-Type: application/json" \
  -d '{"completed": true}'
```

## DSL Advantages

### 1. Declarative Configuration

Define mappings and APIs declaratively:

```kotlin
TodoDto {
  fromAutoLongId(Todo::id)
  from(Todo::title)
}
```

vs. writing manually:

```kotlin
fun Todo.toTodoDto() = TodoDto(
  id = this.id,
  title = this.title
)
```

### 2. Validation Integration

Add validation in DSL:

```kotlin
CreateTodoReq {
  applyTo(Todo::title, field = "title") {
    validation("@NotBlank", "@Size(min = 1, max = 200)")
  }
}
```

### 3. Complete Layer Generation

Generate API interface, controller, and service in one go.

### 4. Independent Regeneration

Regenerate mappings without affecting other code.

## Zygarde DSL Workflow

### 1. Create Generation Scripts for Zygarde

Automate Zygarde code generation:

```bash
#!/bin/bash
# scripts/generate-zygarde-code.sh

echo "Generating Zygarde DAOs (KAPT)..."
./gradlew :todo-domain:kaptKotlin

echo "Generating Zygarde model mappings..."
./gradlew :todo-codegen:run \
  -Dzygarde.model.mapping.codegen.spec.class=com.example.todo.codegen.dsl.TodoModelDsl

echo "Generating Zygarde web API..."
./gradlew :todo-codegen:run \
  -Dzygarde.webmvc.codegen.spec.class=com.example.todo.codegen.dsl.TodoApiDsl

echo "Zygarde code generation complete!"
```

### 2. Zygarde Module Dependencies

Ensure proper order for Zygarde modules:

```kotlin
// settings.gradle.kts
todo-web → todo-service → todo-codegen → todo-domain
```

The generated `Dao` class depends on all entity DAOs from `todo-domain`.

## What You've Learned

✅ Using Zygarde's model mapping DSL
✅ Generating DTOs with Zygarde
✅ Using Zygarde's web API DSL
✅ Multi-module structure for Zygarde
✅ DSL-based Zygarde workflow

## Next Steps

- **[Model Mapping →](../guide/model-mapping.md)** - Advanced mapping patterns
- **[Web & REST →](../guide/web-rest.md)** - API best practices
- **[Reference →](../reference/dsl-properties.md)** - DSL configuration options
- **[Complete Sample →](https://github.com/zygarde-projects/zygarde/tree/v2/samples/todo-multimodule-dsl)** - Full source code

## Troubleshooting

### DSL Generation Fails

Check:
- Zygarde system properties are set correctly
- DSL class exists and extends proper Zygarde base class
- Output directory has write permissions

### Generated Code Not Compiling

Verify:
- All entity types are on classpath
- Validation annotations are available
- Spring dependencies are included

### Module Dependencies

Ensure correct build order:
```bash
./gradlew :todo-domain:build
./gradlew :todo-codegen:build
./gradlew :todo-service:build
./gradlew :todo-web:build
```
