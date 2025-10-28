# Enhanced Search System

Zygarde's Enhanced Search System completely eliminates the verbosity and complexity of JPA Criteria API while maintaining full type safety.

## The Problem with JPA Criteria API

Without Zygarde, building dynamic queries requires verbose, error-prone Criteria API code:

```kotlin
// Standard JPA Criteria API - verbose and hard to maintain
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
if (minYear != null) {
  predicates.add(cb.greaterThanOrEqualTo(root.get("publishedYear"), minYear))
}

query.where(*predicates.toTypedArray())
entityManager.createQuery(query).resultList
```

## Zygarde's Solution: Type-Safe Search DSL

Zygarde generates a fluent DSL that makes complex queries readable and type-safe:

```kotlin
// Zygarde Enhanced Search - concise and maintainable
bookDao.search {
  title() containsIgnoreCase keyword
  author().id() eq authorId
  publishedYear() gte minYear
}
```

**What Zygarde Eliminates:**
- Manual `CriteriaBuilder` instantiation
- String-based field references (`root.get("fieldName")`)
- Manual join creation for relationships
- Predicate collection management
- Type casting and null safety boilerplate

## ZygardeEnhancedDao Interface

Zygarde's enhanced DAO provides five powerful search methods:

```kotlin
interface ZygardeEnhancedDao<T : Any, ID> : BaseDao<T, ID> {
  fun search(block: SearchScope<T, T>.() -> Unit): List<T>
  fun searchOne(block: SearchScope<T, T>.() -> Unit): T?
  fun remove(block: SearchScope<T, T>.() -> Unit): Int
  fun exists(block: SearchScope<T, T>.() -> Unit): Boolean
  fun count(block: SearchScope<T, T>.() -> Unit): Long
}
```

### search() - Find Multiple Entities

Type-safe queries that return lists:

```kotlin
// Simple equality
val kotlinBooks = bookDao.search {
  title() contains "Kotlin"
}

// Multiple conditions with automatic AND
val recentActiveBooks = bookDao.search {
  status() eq Status.ACTIVE
  publishedYear() gte 2020
}

// Relationship traversal (automatic joins)
val usaAuthorBooks = bookDao.search {
  author().country() eq "USA"
  author().status() eq AuthorStatus.ACTIVE
}

// Complex OR conditions
val searchResults = bookDao.search {
  or {
    title() containsIgnoreCase keyword
    description() containsIgnoreCase keyword
    author().name() containsIgnoreCase keyword
  }
}
```

### searchOne() - Find Single Entity

Returns the first matching entity or null:

```kotlin
val book = bookDao.searchOne {
  isbn() eq "978-1234567890"
}

val activeBook = bookDao.searchOne {
  title() eq "Kotlin in Action"
  status() eq Status.ACTIVE
}
```

### remove() - Bulk Delete with DSL

Delete multiple entities matching criteria:

```kotlin
// Returns count of deleted entities
val deletedCount = bookDao.remove {
  publishedYear() lt 2000
  status() eq Status.ARCHIVED
}

// Delete with relationship conditions
val deletedInactiveAuthorBooks = bookDao.remove {
  author().status() eq AuthorStatus.INACTIVE
}
```

### exists() - Check Existence

Fast existence check without loading entities:

```kotlin
val hasKotlinBooks = bookDao.exists {
  title() contains "Kotlin"
}

val hasActiveBooksByAuthor = bookDao.exists {
  author().id() eq authorId
  status() eq Status.ACTIVE
}
```

### count() - Count Matching Entities

Count entities without loading them:

```kotlin
val activeCount = bookDao.count {
  status() eq Status.ACTIVE
}

val recentBooksCount = bookDao.count {
  publishedYear() gte 2020
}
```

## How Code Generation Works

When you annotate an entity with `@ZyModel`:

```kotlin
@Entity
@ZyModel
data class Book(
  @Id @GeneratedValue
  val id: Long? = null,
  val title: String,
  val publishedYear: Int,

  @ManyToOne
  val author: Author
) : AutoLongIdEntity()
```

Zygarde generates:

1. **Enhanced DAO Interface** with search methods
2. **Type-safe field accessors**: `title()`, `publishedYear()`, `author()`
3. **Relationship traversal**: `author().name()`, `author().country()`
4. **Aggregated Dao component** for dependency injection

## Entity Base Classes (Convenience)

Zygarde provides base entity classes to eliminate boilerplate ID and audit field declarations:

```kotlin
// Long auto-increment ID
@Entity
@ZyModel
class Book(var title: String) : AutoLongIdEntity()
// Inherits: id: Long?, @Id, @GeneratedValue, equals(), hashCode()

// Int auto-increment ID
@Entity
@ZyModel
class Category(var name: String) : AutoIntIdEntity()

// With audit fields (requires zygarde-jpa-envers)
@Entity
@ZyModel
@Audited
class AuditedBook(var title: String) : AuditAutoLongIdEntity()
// Inherits: id, createdAt, createdBy, lastModifiedAt, lastModifiedBy
```

## DAO Interfaces

### BaseDao

Basic DAO combining JPA repository features:

```kotlin
interface BaseDao<T, ID> :
  JpaRepository<T, ID>,
  JpaSpecificationExecutor<T>
```

Provides:
- Standard CRUD operations (`save`, `findById`, `findAll`, `delete`)
- Specification-based queries
- Pagination and sorting

### ZygardeEnhancedDao

Extended DAO with additional features:

```kotlin
interface ZygardeEnhancedDao<T : Any, ID> : BaseDao<T, ID> {
  fun search(block: SearchScope<T, T>.() -> Unit): List<T>
  fun searchOne(block: SearchScope<T, T>.() -> Unit): T?
  fun remove(block: SearchScope<T, T>.() -> Unit): Int
  fun exists(block: SearchScope<T, T>.() -> Unit): Boolean
  fun count(block: SearchScope<T, T>.() -> Unit): Long
}
```

#### Enhanced Methods

**search**: Type-safe DSL queries

```kotlin
val books = bookDao.search {
  title() contains "Kotlin"
  author().country() eq "USA"
}
```

**searchOne**: Find single entity

```kotlin
val book = bookDao.searchOne {
  isbn() eq "978-1234567890"
}
```

**remove**: Bulk delete with DSL

```kotlin
val deleted = bookDao.remove {
  publishedYear() lt 2000
  status() eq Status.ARCHIVED
}
// Returns count of deleted entities
```

**exists**: Check existence

```kotlin
val hasKotlinBooks = bookDao.exists {
  title() contains "Kotlin"
}
```

**count**: Count matching entities

```kotlin
val activeCount = bookDao.count {
  status() eq Status.ACTIVE
}
```

## Pagination and Sorting

### Basic Pagination

Using Spring Data's `Pageable`:

```kotlin
import org.springframework.data.domain.PageRequest

val pageable = PageRequest.of(0, 20) // page 0, size 20
val page = bookDao.findAll(pageable)

println("Total pages: ${page.totalPages}")
println("Total elements: ${page.totalElements}")
println("Current page content: ${page.content}")
```

### Pagination with Search DSL

Combine type-safe queries with pagination:

```kotlin
val spec = EnhancedSearchImpl<Book>().apply {
  title() contains "Kotlin"
  status() eq Status.ACTIVE
}.toSpecification()

val pageable = PageRequest.of(0, 20, Sort.by("title").ascending())
val page = bookDao.findAll(spec, pageable)
```

### Zygarde Paging Helpers

```kotlin
import zygarde.core.dto.PagingRequest
import zygarde.core.dto.PagingAndSortingRequest

// Simple paging
val paging = PagingRequest(page = 0, size = 20)
val pageable = paging.toPageable()

// With sorting
val pagingWithSort = PagingAndSortingRequest(
  page = 0,
  size = 20,
  sort = "title,asc;author,desc"
)
val pageableWithSort = pagingWithSort.toPageable()
```

## Advanced Queries

### Projections

Define interface projections for efficient queries:

```kotlin
interface BookSummary {
  val id: Long
  val title: String
  val authorName: String
}

interface BookDao : ZygardeEnhancedDao<Book, Long> {
  @Query("SELECT b.id as id, b.title as title, a.name as authorName " +
         "FROM Book b JOIN b.author a WHERE b.status = :status")
  fun findSummariesByStatus(status: Status): List<BookSummary>
}
```

### Native Queries

Use native SQL when needed:

```kotlin
interface BookDao : ZygardeEnhancedDao<Book, Long> {
  @Query(
    value = "SELECT * FROM books WHERE LOWER(title) LIKE LOWER(:search)",
    nativeQuery = true
  )
  fun searchByTitleNative(search: String): List<Book>
}
```

### Custom Repository Methods

Extend generated DAOs with custom methods:

```kotlin
interface BookDao : ZygardeEnhancedDao<Book, Long> {

  fun findByTitleContainingIgnoreCase(title: String): List<Book>

  fun findByAuthorAndStatus(author: Author, status: Status): List<Book>

  @Query("SELECT b FROM Book b WHERE b.publishedYear BETWEEN :start AND :end")
  fun findByPublishedYearBetween(start: Int, end: Int): List<Book>
}
```

## Relationships

### Lazy vs Eager Loading

```kotlin
@Entity
class Book(
  var title: String,

  // Lazy loading (default for @ManyToOne)
  @ManyToOne(fetch = FetchType.LAZY)
  var author: Author,

  // Eager loading
  @ManyToOne(fetch = FetchType.EAGER)
  var publisher: Publisher,

  // One-to-many (lazy by default)
  @OneToMany(mappedBy = "book", cascade = [CascadeType.ALL])
  var reviews: List<Review> = listOf()
) : AutoLongIdEntity()
```

### Fetch Joins in Queries

Avoid N+1 queries with fetch joins:

```kotlin
interface BookDao : ZygardeEnhancedDao<Book, Long> {

  @Query("SELECT b FROM Book b JOIN FETCH b.author WHERE b.id = :id")
  fun findByIdWithAuthor(id: Long): Book?

  @Query("SELECT DISTINCT b FROM Book b " +
         "LEFT JOIN FETCH b.author " +
         "LEFT JOIN FETCH b.reviews")
  fun findAllWithDetails(): List<Book>
}
```

### Cascading Operations

```kotlin
@Entity
class Author(
  var name: String,

  @OneToMany(
    mappedBy = "author",
    cascade = [CascadeType.ALL],
    orphanRemoval = true
  )
  var books: MutableList<Book> = mutableListOf()
) : AutoLongIdEntity() {

  fun addBook(book: Book) {
    books.add(book)
    book.author = this
  }

  fun removeBook(book: Book) {
    books.remove(book)
    book.author = null
  }
}
```

## Transaction Management

### Basic Transactions

```kotlin
import org.springframework.transaction.annotation.Transactional

@Service
class BookService(private val dao: Dao) {

  @Transactional
  fun createBook(title: String, authorId: Long): Book {
    val author = dao.author.findById(authorId)
      .orElseThrow { IllegalArgumentException("Author not found") }

    val book = Book(title = title, author = author)
    return dao.book.save(book)
  }

  @Transactional(readOnly = true)
  fun getBookWithAuthor(id: Long): Book {
    return dao.book.findByIdWithAuthor(id)
      ?: throw NoSuchElementException("Book not found")
  }
}
```

### Transaction Propagation

```kotlin
@Service
class BookService(private val dao: Dao) {

  @Transactional(propagation = Propagation.REQUIRED)
  fun createBookWithReview(title: String): Book {
    val book = dao.book.save(Book(title = title))
    addReviewInNewTransaction(book.id!!)
    return book
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  fun addReviewInNewTransaction(bookId: Long) {
    val book = dao.book.findById(bookId).orElseThrow()
    val review = Review(book = book, rating = 5)
    dao.review.save(review)
  }
}
```

## Query Performance

### Batch Operations

```kotlin
@Service
class BookService(private val dao: Dao) {

  @Transactional
  fun importBooks(books: List<Book>) {
    val batchSize = 50
    books.chunked(batchSize).forEach { batch ->
      dao.book.saveAll(batch)
      dao.book.flush()
    }
  }
}
```

### Query Hints

```kotlin
interface BookDao : ZygardeEnhancedDao<Book, Long> {

  @QueryHints(
    QueryHint(name = "org.hibernate.cacheable", value = "true"),
    QueryHint(name = "org.hibernate.cacheRegion", value = "book-cache")
  )
  fun findByStatus(status: Status): List<Book>
}
```

### Entity Graphs

Define fetch strategies declaratively:

```kotlin
@Entity
@NamedEntityGraph(
  name = "Book.withAuthorAndPublisher",
  attributeNodes = [
    NamedAttributeNode("author"),
    NamedAttributeNode("publisher")
  ]
)
class Book(/* ... */) : AutoLongIdEntity()

interface BookDao : ZygardeEnhancedDao<Book, Long> {

  @EntityGraph("Book.withAuthorAndPublisher")
  override fun findAll(): List<Book>
}
```

## Next Steps

- **[Search DSL →](search-dsl.md)** - Master the type-safe search DSL
- **[Model Mapping →](model-mapping.md)** - Map entities to DTOs
- **[Common Patterns →](../reference/common-patterns.md)** - Best practices
