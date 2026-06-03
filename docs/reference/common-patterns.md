# Common Patterns

Best practices and common patterns for Zygarde applications.

## Entity Patterns

### Using Entity Base Classes

Choose the appropriate base class for your ID strategy:

```kotlin
// Auto-increment Long ID (recommended for most cases)
@Entity
class Book(var title: String) : AutoLongIdEntity()

// Auto-increment Int ID
@Entity
class Category(var name: String) : AutoIntIdEntity()

// UUID ID
@Entity
class SecureEntity(var data: String) : AutoIdEntity<UUID>() {
  @Id @GeneratedValue
  override var id: UUID? = null
}
```

### Auditing with Envers

Track entity changes automatically:

```kotlin
@Entity
@Audited
class AuditedBook(
  var title: String,
  var author: String
) : AuditAutoLongIdEntity()

// Inherited fields: createdAt, createdBy, lastModifiedAt, lastModifiedBy
```

## Service Layer Patterns

### Service with Aggregated Dao

Use the aggregated Dao for clean service code:

```kotlin
@Service
class BookService(private val dao: Dao) {

  @Transactional(readOnly = true)
  fun getBookWithAuthor(id: Long): BookDto {
    val book = dao.book.findById(id).orElseThrow()
    val author = book.author
    return book.toBookDto(author.toAuthorDto())
  }
}
```

### Transaction Management

Use appropriate transaction settings:

```kotlin
@Service
class BookService(private val dao: Dao) {

  // Read-only for queries
  @Transactional(readOnly = true)
  fun searchBooks(criteria: SearchCriteria): List<BookDto> {
    return dao.book.search {
      /* search criteria */
    }.map { it.toBookDto() }
  }

  // Read-write for modifications
  @Transactional
  fun createBook(request: CreateBookReq): BookDto {
    val book = Book().applyFrom(request)
    return dao.book.save(book).toBookDto()
  }
}
```

## DAO Patterns

### Search with Pagination

Combine search DSL with pagination:

```kotlin
fun searchBooks(
  title: String?,
  pageable: Pageable
): Page<BookDto> {

  val spec = EnhancedSearchImpl<Book>().apply {
    title?.let { title() containsIgnoreCase it }
  }.toSpecification()

  return dao.book.findAll(spec, pageable)
    .map { it.toBookDto() }
}
```

### Reusable Specifications

Create reusable search specifications:

```kotlin
object BookSpecs {

  fun published() = EnhancedSearchImpl<Book>().apply {
    status() eq Status.PUBLISHED
  }.toSpecification()

  fun byAuthor(authorId: Long) = EnhancedSearchImpl<Book>().apply {
    author().id() eq authorId
  }.toSpecification()

  fun recentBooks(days: Int) = EnhancedSearchImpl<Book>().apply {
    createdAt() gte LocalDateTime.now().minusDays(days.toLong())
  }.toSpecification()
}

// Combine specifications
val books = dao.book.findAll(
  BookSpecs.published()
    .and(BookSpecs.recentBooks(30))
)
```

## DTO Patterns

### Separation of Concerns

Use different DTOs for different operations:

```kotlin
// Read DTO - all fields
data class BookDto(
  val id: Long,
  val title: String,
  val description: String?,
  val price: Double,
  val createdAt: LocalDateTime,
  val updatedAt: LocalDateTime
)

// Create DTO - user-provided fields only
data class CreateBookReq(
  val title: String,
  val description: String?,
  val price: Double
)

// Update DTO - updatable fields only
data class UpdateBookReq(
  val title: String?,
  val description: String?,
  val price: Double?
)

// Summary DTO - minimal fields for lists
data class BookSummaryDto(
  val id: Long,
  val title: String,
  val price: Double
)
```

### Hierarchical DTOs

Use inheritance for shared fields:

```kotlin
// Base DTO
abstract class BaseEntityDto(
  open val id: Long,
  open val createdAt: LocalDateTime,
  open val updatedAt: LocalDateTime
)

// Specific DTOs
data class BookDto(
  override val id: Long,
  val title: String,
  val price: Double,
  override val createdAt: LocalDateTime,
  override val updatedAt: LocalDateTime
) : BaseEntityDto(id, createdAt, updatedAt)
```

## Controller Patterns

### Standard CRUD Controller

Implement consistent CRUD operations:

```kotlin
@RestController
@RequestMapping("/api/books")
class BookController(private val bookService: BookService) {

  @GetMapping
  fun list(@PageableDefault(size = 20) pageable: Pageable) =
    bookService.getAllBooks(pageable)

  @GetMapping("/{id}")
  fun get(@PathVariable id: Long) =
    bookService.getBookById(id)

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  fun create(@Valid @RequestBody request: CreateBookReq) =
    bookService.createBook(request)

  @PutMapping("/{id}")
  fun update(@PathVariable id: Long, @Valid @RequestBody request: UpdateBookReq) =
    bookService.updateBook(id, request)

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun delete(@PathVariable id: Long) =
    bookService.deleteBook(id)
}
```

### Search Endpoint

Provide flexible search capabilities:

```kotlin
@GetMapping("/search")
fun search(
  @RequestParam(required = false) title: String?,
  @RequestParam(required = false) authorId: Long?,
  @RequestParam(required = false) minPrice: Double?,
  @RequestParam(required = false) maxPrice: Double?,
  @PageableDefault(size = 20, sort = ["title"]) pageable: Pageable
): PageDto<BookDto> {
  return bookService.searchBooks(
    title = title,
    authorId = authorId,
    minPrice = minPrice,
    maxPrice = maxPrice,
    pageable = pageable
  )
}
```

## Exception Handling Patterns

### Domain Exceptions

Create domain-specific exceptions:

```kotlin
sealed class BookException(message: String, code: String) :
  BusinessException(message, code)

class BookNotFoundException(id: Long) :
  BookException("Book not found: $id", "BOOK_NOT_FOUND")

class InvalidISBNException(isbn: String) :
  BookException("Invalid ISBN: $isbn", "INVALID_ISBN")

class BookAlreadyExistsException(isbn: String) :
  BookException("Book with ISBN $isbn already exists", "BOOK_EXISTS")
```

### Exception Mapping

Map exceptions globally:

```kotlin
@Component
class NoSuchElementExceptionMapper :
  ExceptionToBusinessExceptionMapper<NoSuchElementException>() {

  override fun supported(t: Throwable): Boolean = t is NoSuchElementException

  override fun transform(t: NoSuchElementException): BusinessException {
    return BusinessException(ApiErrorCode.NOT_FOUND, t.message ?: "Resource not found")
  }
}
```

## Validation Patterns

### Entity-Level Validation

```kotlin
@Entity
class Book(
  @Column(nullable = false)
  @field:NotBlank
  @field:Size(min = 1, max = 200)
  var title: String,

  @Column(nullable = false)
  @field:DecimalMin("0.01")
  @field:DecimalMax("9999.99")
  var price: Double
) : AutoLongIdEntity()
```

### DTO-Level Validation

```kotlin
data class CreateBookReq(
  @field:NotBlank(message = "Title is required")
  @field:Size(min = 1, max = 200, message = "Title must be 1-200 characters")
  val title: String,

  @field:NotNull(message = "Price is required")
  @field:DecimalMin(value = "0.01", message = "Price must be positive")
  @field:DecimalMax(value = "9999.99", message = "Price must be less than 10000")
  val price: Double,

  @field:ISBN
  val isbn: String?
)
```

## Testing Patterns

### Service Tests

```kotlin
@SpringBootTest
class BookServiceTest {

  @Autowired
  lateinit var bookService: BookService

  @Autowired
  lateinit var dao: Dao

  @Test
  @Transactional
  fun `should create and retrieve book`() {
    // given
    val request = CreateBookReq(
      title = "Test Book",
      price = 29.99
    )

    // when
    val created = bookService.createBook(request)
    val retrieved = bookService.getBookById(created.id!!)

    // then
    retrieved.title shouldBe "Test Book"
    retrieved.price shouldBe 29.99
  }

  @AfterEach
  fun cleanup() {
    dao.book.deleteAll()
  }
}
```

### Controller Tests

```kotlin
@WebMvcTest(BookController::class)
class BookControllerTest {

  @Autowired
  lateinit var mockMvc: MockMvc

  @MockkBean
  lateinit var bookService: BookService

  @Test
  fun `should create book`() {
    // given
    val request = CreateBookReq(title = "New Book", price = 39.99)
    val expected = BookDto(id = 1L, title = "New Book", price = 39.99)
    every { bookService.createBook(request) } returns expected

    // when & then
    mockMvc.post("/api/books") {
      contentType = MediaType.APPLICATION_JSON
      content = objectMapper.writeValueAsString(request)
    }.andExpect {
      status { isOk() }
      jsonPath("$.id") { value(1) }
      jsonPath("$.title") { value("New Book") }
    }
  }
}
```

## Configuration Patterns

### Application Profiles

```yaml
# application.yml (common settings)
spring:
  jpa:
    show-sql: false
    properties:
      hibernate:
        format_sql: true

---
# application-dev.yml (development)
spring:
  config:
    activate:
      on-profile: dev
  datasource:
    url: jdbc:h2:mem:testdb
  jpa:
    show-sql: true
    hibernate:
      ddl-auto: create-drop

---
# application-prod.yml (production)
spring:
  config:
    activate:
      on-profile: prod
  datasource:
    url: ${DATABASE_URL}
  jpa:
    show-sql: false
    hibernate:
      ddl-auto: validate
```

## Performance Patterns

### Lazy Loading with Fetch Joins

Avoid N+1 queries:

```kotlin
@Query("SELECT b FROM Book b JOIN FETCH b.author WHERE b.id IN :ids")
fun findByIdWithAuthor(ids: List<Long>): List<Book>
```

### Batch Operations

Process large datasets efficiently:

```kotlin
@Transactional
fun importBooks(books: List<Book>) {
  books.chunked(100).forEach { batch ->
    dao.book.saveAll(batch)
    dao.book.flush()
  }
}
```

### Projection Queries

Fetch only needed fields:

```kotlin
interface BookSummary {
  val id: Long
  val title: String
}

@Query("SELECT b.id as id, b.title as title FROM Book b WHERE b.status = :status")
fun findSummariesByStatus(status: Status): List<BookSummary>
```

## See Also

- [JPA Extensions →](../guide/jpa-extensions.md) - JPA features
- [Search DSL →](../guide/search-dsl.md) - Query patterns
- [Web & REST →](../guide/web-rest.md) - API patterns
- [Tutorials →](../tutorials/kapt-based.md) - Complete examples
