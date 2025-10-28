# Frequently Asked Questions

Common questions about using Zygarde framework.

## General Questions

### What is Zygarde?

Zygarde is a Kotlin framework that eliminates boilerplate code in Spring Boot JPA applications through compile-time code generation and a type-safe search DSL. It automatically generates DAOs, search capabilities, and model mapping code from annotated entities.

### Why use Zygarde instead of plain Spring Data JPA?

**Spring Data JPA alone:**
```kotlin
interface BookRepository : JpaRepository<Book, Long> {
  fun findByTitleContainingIgnoreCaseAndAuthorCountryAndPublishedYearGreaterThanEqual(
    title: String, country: String, year: Int
  ): List<Book>
}
```

**With Zygarde:**
```kotlin
dao.book.search {
  title() containsIgnoreCase title
  author().country() eq country
  publishedYear() gte year
}
```

Zygarde provides type-safe, composable queries without verbose method names or manual Specifications.

### Is Zygarde production-ready?

Yes. Zygarde generates standard JPA code at compile-time with zero runtime overhead. It's used in production enterprise applications.

### What's the learning curve?

Low. If you know Spring Boot and JPA, you can start using Zygarde in minutes. The DSL is intuitive and similar to natural language.

## Technical Questions

### How does code generation work?

Zygarde uses Kotlin Annotation Processing Tool (KAPT) to analyze entities marked with `@ZyModel` and generate DAO interfaces and search DSL extensions at compile-time.

```kotlin
@Entity
@ZyModel  // ← Triggers generation
data class Book(/* ... */)

// After build, you get:
// - BookDao interface
// - Search DSL: title(), author(), etc.
// - Aggregated Dao component
```

### Does Zygarde affect runtime performance?

No. All code generation happens at compile-time. Generated code is standard JPA/Hibernate code with no runtime overhead or reflection.

### Can I use Zygarde with existing JPA entities?

Yes! Just add `@ZyModel` annotation to your entities and rebuild. Your existing code continues to work - Zygarde adds capabilities without breaking anything.

### Does it work with Hibernate?

Yes. Zygarde builds on standard JPA, so it works with any JPA provider including Hibernate (the most common choice with Spring Boot).

### What Kotlin version is required?

Zygarde requires Kotlin 1.8 or higher. It's fully compatible with Kotlin 1.9 and 2.0.

### What Spring Boot version is supported?

Zygarde works with Spring Boot 2.7+ and Spring Boot 3.x. It's compatible with both javax.persistence (Boot 2) and jakarta.persistence (Boot 3).

## Usage Questions

### Can I still write custom queries?

Yes! Zygarde doesn't prevent custom queries. You can:
- Use search DSL for common cases
- Write `@Query` methods when needed
- Use native SQL for complex cases
- Mix all approaches

```kotlin
interface BookDao : JpaRepository<Book, Long> {
  // Custom JPQL query
  @Query("SELECT b FROM Book b WHERE b.rating > :minRating")
  fun findHighlyRated(@Param("minRating") rating: Double): List<Book>
}

// Also use search DSL
dao.book.search { /* ... */ }
```

### How do I handle optional search criteria?

Use Kotlin's null-safe operators:

```kotlin
fun searchBooks(
  title: String?,
  authorId: Long?,
  minPrice: Double?
): List<Book> {
  return dao.book.search {
    title?.let { title() containsIgnoreCase it }
    authorId?.let { author().id() eq it }
    minPrice?.let { price() gte it }
  }
}
```

Only non-null criteria are added to the query.

### Can I use pagination with search DSL?

Yes. Convert search to Specification:

```kotlin
fun searchBooksPaged(keyword: String, pageable: Pageable): Page<Book> {
  val spec = dao.book.toSpecification {
    title() containsIgnoreCase keyword
  }
  return dao.book.findAll(spec, pageable)
}
```

### How do I sort search results?

Use `orderBy`:

```kotlin
dao.book.search {
  category() eq "Programming"
  orderBy(Book::publishedYear, desc = true)
  orderBy(Book::title, desc = false)
}
```

### Can I join across multiple tables?

Yes. Zygarde automatically creates joins:

```kotlin
// Book → Author → Country (2 joins)
dao.book.search {
  author().country().name() eq "United States"
}
```

### How do I handle one-to-many relationships?

Query from the "many" side:

```kotlin
// Find books by author
dao.book.search {
  author().id() eq authorId
}

// Or from "one" side (requires custom query)
val author = dao.author.findById(id).get()
val books = author.books  // Lazy loaded
```

### Can I use aggregations (count, sum, etc.)?

For count with criteria (if using Enhanced DAO):

```kotlin
val count = dao.book.count {
  publishedYear() gte 2020
}
```

For complex aggregations, use custom `@Query`:

```kotlin
@Query("SELECT AVG(b.price) FROM Book b WHERE b.publishedYear = :year")
fun averagePriceByYear(@Param("year") year: Int): Double
```

## Multi-Module Projects

### How do I structure a multi-module project?

Separate domain, codegen, and application:

```
my-app/
├── my-app-domain/      # Entities with @ZyModel
├── my-app-codegen/     # Generated code target
└── my-app-service/     # Business logic
```

See [Project Setup](getting-started/project-setup.md) for details.

### Where is generated code placed?

By default: `build/generated/source/kapt/main/`

For multi-module projects, configure the target:

```kotlin
kapt {
  arguments {
    arg("zygarde.codegen.base.package", "com.example.codegen")
  }
}
```

### Can I customize generated code location?

Yes, via KAPT arguments. See [KAPT Options](reference/kapt-options.md).

## Troubleshooting

### Generated code not found after build

**Solution:**
1. Ensure KAPT is configured: `kotlin("kapt")` plugin
2. Rebuild: `./gradlew clean build`
3. Check `build/generated/source/kapt/main/`
4. Verify IDE recognizes generated sources

IntelliJ IDEA usually auto-detects, but you can mark the directory as "Generated Sources Root."

### "Cannot find Dao class"

**Common causes:**
1. Haven't run build yet → Run `./gradlew build`
2. Wrong package name → Check KAPT configuration
3. IDE not updated → File → Invalidate Caches / Restart

### Search DSL methods not available

**Check:**
1. Entity has `@ZyModel` annotation
2. Project was rebuilt after adding annotation
3. Import statement is correct
4. IDE indices are up-to-date

### Type mismatch errors in search DSL

**Cause:** Usually incorrect operator for field type.

**Solution:** Use appropriate operator:
- Strings: `contains`, `like`, `eq`
- Numbers: `eq`, `gt`, `gte`, `lt`, `lte`, `between`
- Booleans: `eq`
- References: `eq` (for ID comparison)

### N+1 query problem

**Solution:** Use fetch joins or EntityGraph:

```kotlin
interface BookDao : JpaRepository<Book, Long> {
  @EntityGraph(attributePaths = ["author"])
  override fun findAll(): List<Book>
}
```

See [Relationships](examples/relationships.md) for details.

## Integration Questions

### Can I use Zygarde with GraphQL?

Yes. Use Zygarde for data access, expose via GraphQL resolvers:

```kotlin
@DgsData(parentType = "Query", field = "books")
fun getBooks(@InputArgument filter: BookFilter): List<BookDto> {
  return dao.book.search {
    filter.title?.let { title() containsIgnoreCase it }
    filter.authorId?.let { author().id() eq it }
  }.map { it.toDto() }
}
```

### Does it work with Kotlin Coroutines?

Zygarde itself is synchronous (follows JPA model). For coroutines, wrap in `withContext`:

```kotlin
suspend fun searchBooks(keyword: String) = withContext(Dispatchers.IO) {
  dao.book.search {
    title() containsIgnoreCase keyword
  }
}
```

Or use Spring Data R2DBC for reactive applications (Zygarde is JPA-focused).

### Can I use it with MongoDB or NoSQL?

No. Zygarde is designed for JPA/SQL databases. For MongoDB, use Spring Data MongoDB's query DSL.

### Does it support multi-tenancy?

Yes. Zygarde works with Hibernate's multi-tenancy features. The generated DAOs respect the configured tenant context.

### Can I use with Micronaut instead of Spring?

Zygarde is Spring-focused. For Micronaut, consider Micronaut Data or QueryDSL.

## Zygarde Usage Patterns

### Should I use Enhanced DAO or basic JpaRepository?

Choose based on Zygarde features you need:

- **Enhanced DAO**: Adds `count()` and `remove()` with search criteria
- **Basic JpaRepository**: Lighter, standard Spring Data operations

Configure via KAPT:
```kotlin
kapt {
  arguments {
    arg("zygarde.codegen.dao.inherit", "zygarde.data.jpa.ZygardeEnhancedDao")
  }
}
```

### How do I organize Zygarde search logic?

**Option 1: Service layer with injected Dao**
```kotlin
@Service
class BookSearchService(private val dao: Dao) {
  fun searchBooks(criteria: BookCriteria) = dao.book.search {
    // Zygarde search logic
  }
}
```

**Option 2: Extension functions on Dao**
```kotlin
fun Dao.searchActiveBooks() = book.search {
  active() eq true
}
```

### Using Zygarde with DTOs

Combine search DSL with model mapping:

```kotlin
fun searchBooks(keyword: String): List<BookDto> {
  return dao.book.search {
    title() containsIgnoreCase keyword
  }.map { it.toBookDto() }  // Use generated mapping
}
```

### Testing Zygarde-Generated Code

Test the generated DAOs like standard Spring Data:

```kotlin
@DataJpaTest
class BookSearchTest {
  @Autowired
  lateinit var dao: Dao  // Zygarde-generated
  
  @Test
  fun `should find books using search DSL`() {
    val results = dao.book.search {
      title() containsIgnoreCase "kotlin"
    }
    
    assertThat(results).hasSize(2)
  }
}
```

## Migration Questions

### Can I migrate gradually?

Yes. Add `@ZyModel` to entities one at a time. Existing repositories continue to work alongside Zygarde DAOs.

### How long does migration take?

Depends on project size:
- Small (< 20 entities): 1-2 days
- Medium (20-50 entities): 1 week
- Large (> 50 entities): 2-4 weeks

Most time is spent migrating custom Specifications to search DSL.

### Do I need to rewrite all my repositories?

No. You can:
1. Keep existing repositories
2. Add Zygarde for new entities
3. Gradually migrate queries to search DSL
4. Delete repositories only when fully migrated

## Still Have Questions?

- **[GitHub Issues](https://github.com/zygarde-projects/zygarde/issues)** - Report bugs or ask questions
- **[Examples](examples/basic-crud.md)** - See practical code examples
- **[Tutorials](tutorials/kapt-based.md)** - Step-by-step guides
