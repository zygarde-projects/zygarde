# Welcome to Zygarde

**Zygarde** is a Kotlin framework designed to **eliminate boilerplate code** while maintaining type safety in Spring Boot applications. Instead of writing repetitive DAO interfaces, search specifications, DTO mappings, and REST controllers, Zygarde generates them automatically through annotation processing and declarative DSLs.

## What Problems Does Zygarde Solve?

### ❌ Without Zygarde
```kotlin
// Verbose Criteria API
val cb = entityManager.criteriaBuilder
val query = cb.createQuery(Book::class.java)
val root = query.from(Book::class.java)
val predicates = mutableListOf<Predicate>()

if (title != null) {
  predicates.add(cb.like(cb.lower(root.get("title")), "%${title.lowercase()}%"))
}
if (authorId != null) {
  val authorJoin = root.join<Book, Author>("author")
  predicates.add(cb.equal(authorJoin.get<Long>("id"), authorId))
}

query.where(*predicates.toTypedArray())
// ... more boilerplate
```

### ✅ With Zygarde's Enhanced Search System
```kotlin
bookDao.search {
  title() containsIgnoreCase searchTerm
  author().id() eq authorId
}
```

## Core Framework Features

### 🔍 Enhanced Search System

A fluent, type-safe DSL that **completely eliminates JPA Criteria API verbosity**:

- **Type-safe field references** - Compile-time validation prevents field name typos
- **Automatic join resolution** - Traverse relationships without manual join creation
- **Chainable conditions** - Build complex queries with readable syntax
- **Zero runtime overhead** - Compiles to optimized JPA Criteria queries

```kotlin
// Complex query with joins, OR conditions, and comparisons
bookDao.search {
  or {
    title() contains "Kotlin"
    description() contains "Kotlin"
  }
  author().country() eq "USA"
  publishedYear() gte 2020
  price() between (10.0 to 50.0)
}
```

### 🏗️ Annotation-Based Code Generation

**KAPT processing** automatically generates from `@ZyModel` entities:

- DAO interfaces with enhanced search capabilities
- Type-safe search DSL extension functions
- Aggregated Dao component for dependency injection

**One annotation replaces hundreds of lines of boilerplate.**

### 📝 Model Mapping DSL

Declarative DTO transformations **without manual mapping code**:

```kotlin
BookDto {
  fromAutoLongId(Book::id)
  from(Book::title)
  fromRef(Book::author) { it.toAuthorDto() }
}

CreateBookReq {
  applyTo(Book::title) {
    validation("@NotBlank", "@Size(min=1, max=200)")
  }
}
```

Generates: `book.toBookDto()` and `book.applyFrom(request)` extension functions.

### 🌐 Web API Generation

Generate complete REST API layers from DSL specifications:

```kotlin
api("BookApi") {
  basePath = "/api/books"

  endpoint {
    name = "searchBooks"
    method = GET
    params = listOf("title" to "String?")
    returns = "List<BookDto>"
  }
}
```

Generates: API interface, controller implementation, and service interface.

## Framework Philosophy

Zygarde follows **three core principles**:

### 1. Eliminate Boilerplate, Not Control

Generate repetitive code automatically while keeping business logic explicit. You write entities and DSL specs, Zygarde generates infrastructure.

### 2. Type Safety Without Verbosity

Use Kotlin's type system for compile-time validation without sacrificing readability. Queries look like natural language but compile to type-checked code.

### 3. Composition Over Configuration

Extend framework components through inheritance and composition rather than extensive configuration files.

## Quick Example

### Step 1: Annotate Your Entity

```kotlin
@Entity
@ZyModel
data class Book(
  @Id @GeneratedValue
  val id: Long? = null,
  val title: String,
  @ManyToOne
  val author: Author
) : AutoLongIdEntity()
```

### Step 2: Build → Zygarde Generates

- `BookDao` interface
- Search DSL functions: `title()`, `author()`
- Aggregated `Dao` component

### Step 3: Use Type-Safe Search

```kotlin
@Service
class BookService(private val dao: Dao) {

  fun searchBooks(keyword: String) = dao.book.search {
    or {
      title() containsIgnoreCase keyword
      description() containsIgnoreCase keyword
    }
    author().status() eq AuthorStatus.ACTIVE
  }
}
```

**Zero lines of manual DAO/Specification code written.**

## Code Generation Pipeline

```
┌─────────────┐
│  @ZyModel   │ Entity Annotations
│  Entities   │
└──────┬──────┘
       │
       │ KAPT Processing
       ▼
┌─────────────┐
│  Generated  │ • DAO Interfaces
│   Code      │ • Search DSL Extensions
└──────┬──────┘ • Aggregated Dao
       │
       │ Compile Time
       ▼
┌─────────────┐
│ Application │ Type-Safe, Zero Runtime Overhead
│    Code     │
└─────────────┘
```

## Modular Architecture

Zygarde's modular design lets you use only what you need:

| Module | Purpose | When to Use |
|--------|---------|-------------|
| **zygarde-jpa** | Enhanced Search DSL | Always (core feature) |
| **zygarde-jpa-codegen** | KAPT processor | Always (generates code) |
| **zygarde-model-mapping** | DTO mappings | When using DTOs |
| **zygarde-webmvc-codegen-dsl** | API generation | For REST APIs |

See [Module Reference](reference/modules.md) for complete list.

## Who Should Use Zygarde?

Zygarde is perfect for teams building:

- **Enterprise Spring Boot applications** with complex domain models
- **REST APIs** requiring type-safe querying and consistent error handling
- **Data-intensive applications** with sophisticated search requirements
- **Multi-tenant systems** needing flexible data access layers

## Real-World Use Cases

### Type-Safe Search Without Boilerplate

**Traditional Approach:**
```kotlin
// Verbose Criteria API or error-prone Specifications
val cb = em.criteriaBuilder
val query = cb.createQuery(Book::class.java)
val root = query.from(Book::class.java)
// ... 20+ lines of boilerplate
```

**With Zygarde:**
```kotlin
dao.book.search {
  title() containsIgnoreCase keyword
  author().country() eq "USA"
  price() between (10.0 to 50.0)
}
```

### Eliminate Manual DAO Creation

**No More Writing:**
- Repository interfaces for every entity
- Custom specifications for searches
- DTO mapping code
- REST controller boilerplate

**Zygarde Generates:**
- Complete DAO with CRUD operations
- Type-safe search DSL
- Model mapping extensions
- REST API layers (optional)

## Framework Integration

Zygarde seamlessly integrates with your Spring Boot stack:

```kotlin
dependencies {
  // Spring Boot (your choice of version)
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-web")
  
  // Add Zygarde
  implementation("zygarde:zygarde-jpa:VERSION")
  kapt("zygarde:zygarde-jpa-codegen:VERSION")
}
```

Works with:
- ✅ Spring Boot 2.x & 3.x
- ✅ Hibernate / JPA
- ✅ Kotlin 1.8+
- ✅ Gradle & Maven
- ✅ Multi-module projects

## Next Steps

<div class="grid cards" markdown>

-   :material-clock-fast:{ .lg .middle } **Getting Started**

    ---

    Install Zygarde and create your first project in 5 minutes

    [:octicons-arrow-right-24: Quick Start](getting-started/quick-start.md)

-   :material-code-braces:{ .lg .middle } **Practical Examples**

    ---

    Learn through real-world code examples

    [:octicons-arrow-right-24: Basic CRUD](examples/basic-crud.md)

-   :material-book-open-variant:{ .lg .middle } **Core Concepts**

    ---

    Understand Zygarde's architecture and design

    [:octicons-arrow-right-24: Architecture](guide/architecture.md)

-   :material-school:{ .lg .middle } **Tutorials**

    ---

    Build complete applications step-by-step

    [:octicons-arrow-right-24: KAPT Tutorial](tutorials/kapt-based.md)

</div>

## Performance & Production Ready

Zygarde is designed for production use:

- **Zero runtime overhead** - Code generation happens at compile-time
- **Type safety** - All queries validated at compile-time
- **JPA optimized** - Generated code uses best practices
- **Battle-tested** - Used in production enterprise applications
- **Kotlin-first** - Built for modern Kotlin applications

## License

Zygarde is open source and available under the [Apache 2.0 License](https://www.apache.org/licenses/LICENSE-2.0).
