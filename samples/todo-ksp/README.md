# Todo KSP Sample

This sample demonstrates using the `zygarde-jpa-codegen-ksp` module for generating JPA repository interfaces and search extensions using KSP.

## Features Demonstrated

- Entity definition with `@ZyModel` annotation
- Automatic DAO generation (`TodoDao`)
- Type-safe search extensions (`description()`, `completed()`, etc.)
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

## Usage Example

```kotlin
@Service
class TodoService(@Autowired val todoDao: TodoDao) {
    fun searchByDescription(description: String): List<Todo> {
        return todoDao.search {
            description() eq description
        }
    }
}
```
