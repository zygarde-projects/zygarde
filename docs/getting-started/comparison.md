# Comparison with Alternatives

Understand how Zygarde compares to other approaches and when to use it.

## vs. JPA Criteria API

### JPA Criteria API

```kotlin
// Verbose and error-prone
val cb = entityManager.criteriaBuilder
val query = cb.createQuery(Book::class.java)
val root = query.from(Book::class.java)
val predicates = mutableListOf<Predicate>()

if (title != null) {
  predicates.add(
    cb.like(
      cb.lower(root.get("title")), 
      "%${title.lowercase()}%"
    )
  )
}

val authorJoin = root.join<Book, Author>("author")
predicates.add(cb.equal(authorJoin.get<Long>("id"), authorId))

query.where(*predicates.toTypedArray())
val results = entityManager.createQuery(query).resultList
```

**Problems:**
- ❌ Verbose and repetitive
- ❌ String-based field references (no compile-time safety)
- ❌ Manual join management
- ❌ Complex predicate building

### Zygarde

```kotlin
// Clean and type-safe
dao.book.search {
  title()?.let { it containsIgnoreCase title }
  author().id() eq authorId
}
```

**Benefits:**
- ✅ Concise and readable
- ✅ Type-safe field references
- ✅ Automatic join resolution
- ✅ Compile-time validation

## vs. Spring Data Specifications

### Spring Data Specifications

```kotlin
class BookSpecifications {
  companion object {
    fun titleContains(title: String): Specification<Book> {
      return Specification { root, query, cb ->
        cb.like(cb.lower(root.get("title")), "%${title.lowercase()}%")
      }
    }
    
    fun byAuthorId(authorId: Long): Specification<Book> {
      return Specification { root, query, cb ->
        val authorJoin = root.join<Book, Author>("author")
        cb.equal(authorJoin.get<Long>("id"), authorId)
      }
    }
  }
}

// Usage
val spec = BookSpecifications.titleContains("kotlin")
  .and(BookSpecifications.byAuthorId(authorId))
repository.findAll(spec)
```

**Problems:**
- ❌ Requires separate specification classes
- ❌ Still uses string-based field references internally
- ❌ Boilerplate for each query condition
- ❌ Not as fluent

### Zygarde

```kotlin
dao.book.search {
  title() containsIgnoreCase "kotlin"
  author().id() eq authorId
}
```

**Benefits:**
- ✅ No separate specification classes
- ✅ Type-safe throughout
- ✅ Inline query definition
- ✅ Fluent and composable

## vs. QueryDSL

### QueryDSL

```kotlin
// Requires code generation for Q-types
val qBook = QBook.book
val qAuthor = QAuthor.author

val results = queryFactory
  .selectFrom(qBook)
  .join(qBook.author, qAuthor)
  .where(
    qBook.title.containsIgnoreCase("kotlin")
      .and(qAuthor.id.eq(authorId))
  )
  .fetch()
```

**Comparison:**

| Feature | QueryDSL | Zygarde |
|---------|----------|---------|
| Type-safe | ✅ Yes | ✅ Yes |
| Code generation | ✅ Q-types | ✅ DAOs + DSL |
| Learning curve | Medium | Low |
| Spring integration | External | Native |
| DAO generation | ❌ No | ✅ Yes |
| Fluent API | ✅ Yes | ✅ Yes |
| Kotlin-first | ⚠️ Java-focused | ✅ Yes |

**When to use QueryDSL:**
- Need SQL queries (not just JPA)
- Large existing QueryDSL codebase
- Multi-database support

**When to use Zygarde:**
- Kotlin Spring Boot projects
- Want DAO generation
- Prefer Kotlin DSL style
- JPA/Hibernate is sufficient

## vs. jOOQ

### jOOQ

```kotlin
// SQL-first approach
val results = dsl
  .select()
  .from(BOOK)
  .join(AUTHOR).on(BOOK.AUTHOR_ID.eq(AUTHOR.ID))
  .where(
    BOOK.TITLE.containsIgnoreCase("kotlin")
      .and(AUTHOR.ID.eq(authorId))
  )
  .fetchInto(Book::class.java)
```

**Comparison:**

| Feature | jOOQ | Zygarde |
|---------|------|---------|
| Approach | SQL-first | JPA-first |
| Type-safe | ✅ Yes | ✅ Yes |
| Database-specific | ✅ Yes | ❌ No |
| Complex SQL | ✅ Excellent | ⚠️ Limited |
| JPA integration | ⚠️ Separate | ✅ Native |
| Learning curve | High | Low |

**When to use jOOQ:**
- Need database-specific features
- Complex SQL queries
- SQL is your primary abstraction

**When to use Zygarde:**
- JPA/Hibernate is sufficient
- Want JPA entity benefits
- Prefer ORM approach
- Standard CRUD + search

## vs. Exposed (Kotlin SQL)

### Exposed

```kotlin
// DSL approach
Books
  .join(Authors, JoinType.INNER, Books.authorId, Authors.id)
  .select {
    (Books.title like "%kotlin%") and (Authors.id eq authorId)
  }
  .map { resultRow ->
    Book(
      id = resultRow[Books.id],
      title = resultRow[Books.title],
      // ...
    )
  }
```

**Comparison:**

| Feature | Exposed | Zygarde |
|---------|---------|---------|
| Kotlin-native | ✅ Yes | ✅ Yes |
| Type-safe | ✅ Yes | ✅ Yes |
| Spring integration | ⚠️ Manual | ✅ Native |
| JPA compatible | ❌ No | ✅ Yes |
| ORM features | ⚠️ Basic | ✅ Full JPA |
| Learning curve | Medium | Low |

**When to use Exposed:**
- Want pure Kotlin solution
- Don't need JPA
- Building from scratch

**When to use Zygarde:**
- Using Spring Data JPA
- Need JPA features (caching, lazy loading, etc.)
- Existing JPA entities

## vs. Manual Repository Methods

### Manual Approach

```kotlin
interface BookRepository : JpaRepository<Book, Long> {
  fun findByTitleContainingIgnoreCaseAndAuthorId(
    title: String,
    authorId: Long
  ): List<Book>
  
  @Query("SELECT b FROM Book b JOIN b.author a " +
         "WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :title, '%')) " +
         "AND a.id = :authorId")
  fun searchBooks(
    @Param("title") title: String,
    @Param("authorId") authorId: Long
  ): List<Book>
}
```

**Problems:**
- ❌ Long method names or JPQL strings
- ❌ Repetitive for each query
- ❌ Hard to compose dynamically
- ❌ One interface per entity

### Zygarde

```kotlin
// No repository interfaces needed
dao.book.search {
  title() containsIgnoreCase title
  author().id() eq authorId
}
```

**Benefits:**
- ✅ No interfaces to maintain
- ✅ Dynamic composition
- ✅ Single `Dao` for all entities
- ✅ Compile-time safety

## Decision Matrix

### Choose Zygarde When:

✅ Building Spring Boot + JPA applications
✅ Using Kotlin
✅ Want to reduce boilerplate
✅ Need type-safe queries
✅ Standard CRUD + search is sufficient
✅ Prefer DSL over SQL

### Consider Alternatives When:

❌ Need complex SQL features (use jOOQ)
❌ Database-specific optimizations required (use jOOQ)
❌ Not using JPA/Hibernate (use Exposed)
❌ Already heavily invested in QueryDSL
❌ Pure SQL approach preferred (use jOOQ/Exposed)

## Migration Guides

### From Spring Data Specifications

**Before:**
```kotlin
interface BookRepository : JpaSpecificationExecutor<Book>

class BookSpecs {
  fun titleContains(title: String) = Specification<Book> { root, query, cb ->
    cb.like(cb.lower(root.get("title")), "%${title.lowercase()}%")
  }
}

// Usage
bookRepository.findAll(BookSpecs().titleContains("kotlin"))
```

**After:**
```kotlin
@Entity
@ZyModel  // Add annotation
data class Book(/* ... */)

// Build to generate DAO

// Usage
dao.book.search {
  title() containsIgnoreCase "kotlin"
}
```

**Steps:**
1. Add `@ZyModel` to entities
2. Build project to generate DAOs
3. Replace `findAll(spec)` with `dao.entity.search { }`
4. Remove specification classes

### From QueryDSL

**Before:**
```kotlin
queryFactory
  .selectFrom(qBook)
  .where(qBook.title.containsIgnoreCase("kotlin"))
  .fetch()
```

**After:**
```kotlin
dao.book.search {
  title() containsIgnoreCase "kotlin"
}
```

**Steps:**
1. Add Zygarde dependencies
2. Add `@ZyModel` annotations
3. Gradually replace QueryDSL queries
4. Remove QueryDSL once migrated

### From Manual Repositories

**Before:**
```kotlin
interface BookRepository : JpaRepository<Book, Long> {
  fun findByTitle(title: String): List<Book>
  fun findByAuthorId(authorId: Long): List<Book>
  // ... 50 more methods
}
```

**After:**
```kotlin
@Entity
@ZyModel
data class Book(/* ... */)

// All queries via search DSL
dao.book.search { title() eq title }
dao.book.search { author().id() eq authorId }
```

**Steps:**
1. Add `@ZyModel` to entities
2. Replace custom finder methods with search DSL
3. Keep complex `@Query` methods initially
4. Gradually migrate remaining queries

## Feature Comparison Matrix

| Feature | Zygarde | QueryDSL | jOOQ | Exposed | Specs |
|---------|---------|----------|------|---------|-------|
| **Type Safety** | ✅ | ✅ | ✅ | ✅ | ⚠️ |
| **Kotlin DSL** | ✅ | ❌ | ❌ | ✅ | ❌ |
| **DAO Generation** | ✅ | ❌ | ❌ | ❌ | ❌ |
| **JPA Integration** | ✅ | ✅ | ⚠️ | ❌ | ✅ |
| **Spring Native** | ✅ | ⚠️ | ⚠️ | ❌ | ✅ |
| **Learning Curve** | Low | Med | High | Med | Med |
| **Complex SQL** | ⚠️ | ✅ | ✅ | ✅ | ⚠️ |
| **Code Gen Required** | Yes | Yes | Yes | No | No |
| **Boilerplate** | Minimal | Low | Low | Med | High |

## Conclusion

**Zygarde shines when:**
- You're building Kotlin + Spring Boot + JPA applications
- You want to eliminate repository boilerplate
- Type-safe queries are important
- Standard CRUD and search cover 90% of your needs

**Consider alternatives for:**
- Complex SQL requirements → jOOQ
- Non-JPA projects → Exposed
- Existing large QueryDSL codebase → stick with QueryDSL
- Simple apps → manual repositories may suffice

## Next Steps

- **[Quick Start](../getting-started/quick-start.md)** - Try Zygarde in 5 minutes
- **[Architecture](../guide/architecture.md)** - Understand the design
