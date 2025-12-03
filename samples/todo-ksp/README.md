# Todo KSP Sample

This sample demonstrates using all Zygarde KSP code generation modules:
- `zygarde-jpa-codegen-ksp` - DAO and search extensions
- `zygarde-model-mapping-codegen-ksp` - DTOs and mapping extensions
- `zygarde-webmvc-codegen-ksp` - API interfaces, controllers, and services

## Features Demonstrated

- Entity definition with `@ZyModel` annotation
- Automatic DAO generation (`TodoDao`)
- Type-safe search extensions (`description()`, etc.)
- DTO generation with `@ApiProp` annotations
- API generation with `@ZyApi` and `@GenApi`
- Static option API with `@StaticOptionApi`
- Combined Dao class injection

## Running the Sample

```bash
./gradlew :todo-ksp:build
```

## Generated Code

After building, check `build/generated/ksp/main/kotlin/` for:

- `zygarde/samples/todo/generated/dao/TodoDao.kt` - Repository interface
- `zygarde/samples/todo/generated/dao/Dao.kt` - Combined Dao class
- `zygarde/samples/todo/generated/search/TodoExtensions.kt` - Search extensions
- `zygarde/samples/todo/generated/dto/*.kt` - DTOs and mapping extensions
- `zygarde/samples/todo/generated/api/*.kt` - API interfaces and Feign clients
- `zygarde/samples/todo/generated/api/impl/*.kt` - Controllers
- `zygarde/samples/todo/generated/service/*.kt` - Service interfaces

## Migration from KAPT

Compare with `todo-legacy` sample to see the differences:

**KAPT (todo-legacy):**
```kotlin
dependencies {
    kapt(project(":zygarde-jpa-codegen"))
    kapt(project(":zygarde-model-mapping-codegen"))
    kapt(project(":zygarde-webmvc-codegen"))
}
```

**KSP (todo-ksp):**
```kotlin
plugins {
    id("com.google.devtools.ksp")
}

dependencies {
    ksp(project(":zygarde-jpa-codegen-ksp"))
    ksp(project(":zygarde-model-mapping-codegen-ksp"))
    ksp(project(":zygarde-webmvc-codegen-ksp"))
}

ksp {
    arg("zygarde.codegen.base.package", "zygarde.samples.todo.generated")
    arg("zygarde.codegen.dao.package", "dao")
    arg("zygarde.codegen.entity.search", "search")
    arg("zygarde.codegen.dto.package", "dto")
}
```

## Usage Example

```kotlin
@Service
class MyTodoService(@Autowired val todoDao: TodoDao) : TodoService {
    fun searchByDescription(description: String): List<Todo> {
        return todoDao.search {
            description() eq description
        }
    }
    
    override fun createTodo(req: CreateTodoReq): TodoDto {
        return Todo().applyFromCreateTodoReq(req).let(todoDao::save).toTodoDto()
    }
}
```
