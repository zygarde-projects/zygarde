# Model Mapping

Zygarde's model mapping DSL provides a declarative way to define bidirectional mappings between entities and DTOs with automatic code generation.

## Overview

The model mapping DSL generates extension functions that handle the transformation between entities and DTOs, eliminating boilerplate mapping code.

### Mapping Directions

1. **Entity → DTO** (`from`, `fromAutoId`): Generates `.toXxxDto()` extensions
2. **DTO → Entity** (`applyTo`): Generates `.applyFrom(dto)` extensions on entities
3. **Custom transformations**: Via `ValueProvider` implementations

## Basic Mapping

### Simple DTO Mapping

Define a DTO with entity-to-DTO mappings:

```kotlin
import zygarde.codegen.dsl.model.ModelMappingCodegenSpec

class BookModelDsl : ModelMappingCodegenSpec({

  BookDto {
    // Map ID field (auto-increment Long)
    fromAutoLongId(Book::id)

    // Map simple fields
    from(Book::title)
    from(Book::description)
    from(Book::publishedYear)
    from(Book::price)
  }
})
```

Generated code:

```kotlin
fun Book.toBookDto() = BookDto(
  id = this.id,
  title = this.title,
  description = this.description,
  publishedYear = this.publishedYear,
  price = this.price
)
```

Usage:

```kotlin
val book = bookDao.findById(1L).orElseThrow()
val dto = book.toBookDto()
```

### Create Request Mapping

Define DTO-to-entity mappings for create operations:

```kotlin
CreateBookReq {
  applyTo(Book::title)
  applyTo(Book::description)
  applyTo(Book::publishedYear)
  applyTo(Book::price)
}
```

Generated code:

```kotlin
fun Book.applyFrom(dto: CreateBookReq): Book {
  return this.copy(
    title = dto.title,
    description = dto.description,
    publishedYear = dto.publishedYear,
    price = dto.price
  )
}
```

Usage:

```kotlin
@Service
class BookService(private val dao: Dao) {

  @Transactional
  fun createBook(request: CreateBookReq): BookDto {
    val book = Book()
      .applyFrom(request)

    return dao.book.save(book).toBookDto()
  }
}
```

### Update Request Mapping

Similar to create requests, but typically for existing entities:

```kotlin
UpdateBookReq {
  applyTo(Book::title)
  applyTo(Book::description)
  applyTo(Book::price)
}
```

Usage:

```kotlin
@Transactional
fun updateBook(id: Long, request: UpdateBookReq): BookDto {
  val book = dao.book.findById(id).orElseThrow()

  val updated = book.applyFrom(request)
  return dao.book.save(updated).toBookDto()
}
```

## ID Field Mapping

### Auto-Increment IDs

For entities with auto-generated IDs:

```kotlin
// Long ID
BookDto {
  fromAutoLongId(Book::id)
}

// Int ID
CategoryDto {
  fromAutoIntId(Category::id)
}
```

### Custom ID Types

```kotlin
// UUID ID
UserDto {
  from(User::id) // No special handling needed for non-auto-increment
}

// Composite ID
OrderDto {
  from(Order::id) // Custom ID class
}
```

## Relationship Mapping

### Many-to-One Relationships

Map related entities to their DTOs:

```kotlin
BookDto {
  fromAutoLongId(Book::id)
  from(Book::title)

  // Map author relationship
  fromRef(Book::author, "authorDto") { author ->
    author.toAuthorDto()
  }
}

AuthorDto {
  fromAutoLongId(Author::id)
  from(Author::name)
  from(Author::country)
}
```

Generated code:

```kotlin
fun Book.toBookDto() = BookDto(
  id = this.id,
  title = this.title,
  authorDto = this.author.toAuthorDto()
)
```

### One-to-Many Relationships

Map collections:

```kotlin
AuthorDto {
  fromAutoLongId(Author::id)
  from(Author::name)

  // Map book collection
  fromCollection(Author::books, "bookDtos") { book ->
    book.toBookSummaryDto()
  }
}

BookSummaryDto {
  fromAutoLongId(Book::id)
  from(Book::title)
  from(Book::publishedYear)
}
```

### Optional Relationships

Handle nullable relationships:

```kotlin
BookDto {
  fromAutoLongId(Book::id)
  from(Book::title)

  // Optional publisher
  fromRefOptional(Book::publisher, "publisherDto") { publisher ->
    publisher?.toPublisherDto()
  }
}
```

## Custom Transformations

### Value Providers

Create custom transformation logic:

```kotlin
import zygarde.codegen.dsl.model.ValueProvider

class PriceFormatter : ValueProvider<Double, String> {
  override fun invoke(from: Double): String {
    return "$%.2f".format(from)
  }
}

BookDto {
  fromAutoLongId(Book::id)
  from(Book::title)

  // Custom price formatting
  from(Book::price, "formattedPrice", PriceFormatter())
}
```

### Computed Fields

Add computed properties to DTOs:

```kotlin
BookDto {
  fromAutoLongId(Book::id)
  from(Book::title)
  from(Book::price)

  // Computed field
  extra("discountedPrice") { book ->
    book.price * 0.9 // 10% discount
  }
}
```

Generated code:

```kotlin
fun Book.toBookDto() = BookDto(
  id = this.id,
  title = this.title,
  price = this.price,
  discountedPrice = this.price * 0.9
)
```

### Field Renaming

Map to different field names:

```kotlin
BookDto {
  fromAutoLongId(Book::id)

  // Map 'name' in entity to 'title' in DTO
  from(Book::name, targetField = "title")
}
```

## Validation Integration

### Bean Validation

Add validation annotations to DTO fields:

```kotlin
CreateBookReq {
  applyTo(Book::title, field = "title") {
    validation(
      "@NotBlank(message = \"Title is required\")",
      "@Size(min = 1, max = 200, message = \"Title must be between 1 and 200 characters\")"
    )
  }

  applyTo(Book::description, field = "description") {
    validation("@Size(max = 1000)")
  }

  applyTo(Book::price, field = "price") {
    validation(
      "@NotNull",
      "@DecimalMin(value = \"0.0\", inclusive = false)",
      "@DecimalMax(value = \"9999.99\")"
    )
  }

  applyTo(Book::publishedYear, field = "publishedYear") {
    validation(
      "@NotNull",
      "@Min(1000)",
      "@Max(9999)"
    )
  }
}
```

Generated DTO:

```kotlin
data class CreateBookReq(
  @field:NotBlank(message = "Title is required")
  @field:Size(min = 1, max = 200, message = "Title must be between 1 and 200 characters")
  val title: String,

  @field:Size(max = 1000)
  val description: String?,

  @field:NotNull
  @field:DecimalMin(value = "0.0", inclusive = false)
  @field:DecimalMax(value = "9999.99")
  val price: Double,

  @field:NotNull
  @field:Min(1000)
  @field:Max(9999)
  val publishedYear: Int
)
```

### Custom Validators

```kotlin
CreateBookReq {
  applyTo(Book::isbn, field = "isbn") {
    validation("@ISBN")
  }

  applyTo(Book::email, field = "authorEmail") {
    validation(
      "@Email",
      "@Pattern(regexp = \"^[A-Za-z0-9+_.-]+@(.+)$\")"
    )
  }
}
```

## DTO Hierarchies

### Base DTOs

Create reusable base DTO structures:

```kotlin
BaseEntityDto {
  from(BaseEntity::id)
  from(BaseEntity::createdAt)
  from(BaseEntity::updatedAt)
}

BookDto extends BaseEntityDto {
  from(Book::title)
  from(Book::description)
}
```

### Polymorphic DTOs

Handle entity inheritance:

```kotlin
PublicationDto {
  from(Publication::id)
  from(Publication::title)
}

BookDto extends PublicationDto {
  from(Book::isbn)
  from(Book::pageCount)
}

MagazineDto extends PublicationDto {
  from(Magazine::issueNumber)
  from(Magazine::frequency)
}
```

## Pagination DTOs

### Page Response DTOs

Map Spring Data `Page` to custom pagination DTOs:

```kotlin
import zygarde.core.dto.PageDto

fun searchBooks(pageable: Pageable): PageDto<BookDto> {
  val page = bookDao.findAll(pageable)

  return PageDto(
    atPage = page.number,
    totalPages = page.totalPages,
    totalCount = page.totalElements,
    items = page.content.map { it.toBookDto() }
  )
}
```

### Custom Pagination

```kotlin
data class BookPageDto(
  val books: List<BookDto>,
  val page: Int,
  val size: Int,
  val total: Long,
  val hasNext: Boolean
)

fun searchBooksCustom(pageable: Pageable): BookPageDto {
  val page = bookDao.findAll(pageable)

  return BookPageDto(
    books = page.content.map { it.toBookDto() },
    page = page.number,
    size = page.size,
    total = page.totalElements,
    hasNext = page.hasNext()
  )
}
```

## Batch Mapping

### Mapping Collections

```kotlin
fun getAllBooks(): List<BookDto> {
  return bookDao.findAll().map { it.toBookDto() }
}

fun getBooksByAuthor(authorId: Long): List<BookDto> {
  return bookDao.search {
    author().id() eq authorId
  }.map { it.toBookDto() }
}
```

### Optimized Batch Mapping

For large collections, consider pagination:

```kotlin
fun getAllBooksPaged(): Sequence<BookDto> {
  var page = 0
  val size = 100

  return sequence {
    while (true) {
      val pageData = bookDao.findAll(PageRequest.of(page, size))
      yieldAll(pageData.content.map { it.toBookDto() })

      if (!pageData.hasNext()) break
      page++
    }
  }
}
```

## Testing Mappings

### Unit Tests

```kotlin
@Test
fun `should map book to DTO correctly`() {
  // given
  val book = Book(
    id = 1L,
    title = "Kotlin in Action",
    description = "A comprehensive guide",
    price = 49.99,
    publishedYear = 2017
  )

  // when
  val dto = book.toBookDto()

  // then
  dto.id shouldBe 1L
  dto.title shouldBe "Kotlin in Action"
  dto.description shouldBe "A comprehensive guide"
  dto.price shouldBe 49.99
  dto.publishedYear shouldBe 2017
}

@Test
fun `should apply DTO to entity correctly`() {
  // given
  val request = CreateBookReq(
    title = "New Book",
    description = "Description",
    price = 29.99,
    publishedYear = 2024
  )
  val book = Book()

  // when
  val result = book.applyFrom(request)

  // then
  result.title shouldBe "New Book"
  result.description shouldBe "Description"
  result.price shouldBe 29.99
  result.publishedYear shouldBe 2024
}
```

### Integration Tests

```kotlin
@SpringBootTest
class BookMappingIntegrationTest {

  @Autowired
  lateinit var bookService: BookService

  @Test
  fun `should create book from request DTO`() {
    // given
    val request = CreateBookReq(
      title = "Test Book",
      description = "Test",
      price = 39.99,
      publishedYear = 2024
    )

    // when
    val result = bookService.createBook(request)

    // then
    result.id shouldNotBe null
    result.title shouldBe "Test Book"
    result.price shouldBe 39.99
  }
}
```

## Zygarde Model Mapping Patterns

### 1. Separate Read and Write DTOs with Zygarde DSL

```kotlin
// Read DTO - use Zygarde's from() methods
BookDto {
  fromAutoLongId(Book::id)
  from(Book::title)
  from(Book::description)
  from(Book::price)
  from(Book::createdAt)
  from(Book::updatedAt)
}

// Create DTO - use Zygarde's applyTo() methods
CreateBookReq {
  applyTo(Book::title)
  applyTo(Book::description)
  applyTo(Book::price)
}

// Update DTO - partial updates
UpdateBookReq {
  applyTo(Book::description)
  applyTo(Book::price)
}
```

### 2. Use Summary DTOs with Zygarde for Lists

```kotlin
// Detailed DTO with Zygarde's fromRef
BookDetailDto {
  fromAutoLongId(Book::id)
  from(Book::title)
  from(Book::description)
  from(Book::price)
  fromRef(Book::author, "author") { it.toAuthorDetailDto() }
  fromCollection(Book::reviews, "reviews") { it.toReviewDto() }
}

// Summary DTO - minimal fields
BookSummaryDto {
  fromAutoLongId(Book::id)
  from(Book::title)
  from(Book::price)
  from(Book::author, "authorName") { it.name }
}
```

### 3. Combine with Zygarde Search DSL

Use generated mapping with search results:

```kotlin
fun searchBooks(keyword: String): List<BookSummaryDto> {
  return dao.book.search {
    title() containsIgnoreCase keyword
  }.map { it.toBookSummaryDto() }  // Generated by Zygarde
}
```

## Next Steps

- **[Web & REST →](web-rest.md)** - Build REST APIs with DTOs
- **[Code Generation →](code-generation.md)** - Learn DSL code generation
- **[DSL Tutorial →](../tutorials/dsl-based.md)** - Complete DSL example
- **[DSL Properties →](../reference/dsl-properties.md)** - Configuration options
