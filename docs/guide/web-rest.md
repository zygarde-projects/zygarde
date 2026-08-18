# Web & REST Utilities

Zygarde provides utilities for building robust REST APIs with consistent error handling, validation, and response standardization.

## REST API Generation

### Three-Layer Architecture

Zygarde generates three layers for REST APIs:

1. **API Interface**: Contract definition with Spring annotations
2. **Controller Implementation**: HTTP endpoint routing
3. **Service Interface**: Business logic contract

This separation provides:
- Clear contracts
- Easy testing (mock services)
- Consistent API structure

### API Interface

Define your REST API contract:

```kotlin
import org.springframework.web.bind.annotation.*

@RequestMapping("/api/books")
interface BookApi {

  @GetMapping
  fun getAllBooks(): List<BookDto>

  @GetMapping("/{id}")
  fun getBookById(@PathVariable id: Long): BookDto

  @PostMapping
  fun createBook(@Valid @RequestBody request: CreateBookReq): BookDto

  @PutMapping("/{id}")
  fun updateBook(
    @PathVariable id: Long,
    @Valid @RequestBody request: UpdateBookReq
  ): BookDto

  @DeleteMapping("/{id}")
  fun deleteBook(@PathVariable id: Long)
}
```

### Controller Implementation

Implement the API interface:

```kotlin
@RestController
class BookController(
  private val bookService: BookService
) : BookApi {

  override fun getAllBooks() = bookService.getAllBooks()

  override fun getBookById(id: Long) = bookService.getBookById(id)

  override fun createBook(request: CreateBookReq) = bookService.createBook(request)

  override fun updateBook(id: Long, request: UpdateBookReq) =
    bookService.updateBook(id, request)

  override fun deleteBook(id: Long) = bookService.deleteBook(id)
}
```

### Service Interface

Define business logic contract:

```kotlin
interface BookService {
  fun getAllBooks(): List<BookDto>
  fun getBookById(id: Long): BookDto
  fun createBook(request: CreateBookReq): BookDto
  fun updateBook(id: Long, request: UpdateBookReq): BookDto
  fun deleteBook(id: Long)
}
```

### Service Implementation

```kotlin
@Service
class BookServiceImpl(private val dao: Dao) : BookService {

  @Transactional(readOnly = true)
  override fun getAllBooks() = dao.book.findAll().map { it.toBookDto() }

  @Transactional(readOnly = true)
  override fun getBookById(id: Long): BookDto {
    return dao.book.findById(id)
      .orElseThrow { NoSuchElementException("Book not found: $id") }
      .toBookDto()
  }

  @Transactional
  override fun createBook(request: CreateBookReq): BookDto {
    val book = Book().applyFrom(request)
    return dao.book.save(book).toBookDto()
  }

  @Transactional
  override fun updateBook(id: Long, request: UpdateBookReq): BookDto {
    val book = dao.book.findById(id)
      .orElseThrow { NoSuchElementException("Book not found: $id") }

    val updated = book.applyFrom(request)
    return dao.book.save(updated).toBookDto()
  }

  @Transactional
  override fun deleteBook(id: Long) {
    dao.book.deleteById(id)
  }
}
```

## Exception Handling

### Business Exception

Define domain-specific exceptions:

```kotlin
import zygarde.core.exception.BusinessException

class BookNotFoundException(id: Long) :
  BusinessException("Book not found: $id", code = "BOOK_NOT_FOUND")

class InvalidISBNException(isbn: String) :
  BusinessException("Invalid ISBN: $isbn", code = "INVALID_ISBN")
```

### Global Exception Handling

Zygarde provides `ApiExceptionHandler` for consistent error responses. `BusinessException` is handled automatically, and application exceptions can be mapped by registering `ExceptionToBusinessExceptionMapper` beans:

```kotlin
import org.springframework.stereotype.Component
import zygarde.api.exception.ExceptionToBusinessExceptionMapper
import zygarde.core.exception.ApiErrorCode
import zygarde.core.exception.BusinessException

@Component
class NoSuchElementExceptionMapper :
  ExceptionToBusinessExceptionMapper<NoSuchElementException>() {

  override fun supported(t: Throwable): Boolean = t is NoSuchElementException

  override fun transform(t: NoSuchElementException): BusinessException {
    return BusinessException(ApiErrorCode.NOT_FOUND, t.message ?: "Resource not found")
  }
}
```

### Business Exception Logging

By default `ApiExceptionHandler` logs every handled `BusinessException` at `INFO` with the full stack trace.
Since business exceptions are usually expected flow-control responses rather than bugs, this can be tuned with
properties (no code required):

```yaml
zygarde:
  api:
    business-exception-log:
      level: INFO             # TRACE / DEBUG / INFO / WARN / ERROR / OFF (default: INFO)
      include-stack-trace: false  # log the message only, drop the stack trace (default: true)
```

For full control, `logBusinessException` is `protected open` — subclass the handler and register it as the
`ApiExceptionResolver` bean (the default handler backs off via `@ConditionalOnMissingBean`):

```kotlin
@ControllerAdvice
class AppApiExceptionHandler : ApiExceptionHandler() {
  override fun logBusinessException(e: BusinessException) {
    LOGGER.info("{} {}", e.code, e.message)
  }
}
```

To avoid paying the cost of capturing a stack trace at all, create the exception with
`BusinessException.noStackTrace(...)`, or subclass `BusinessException` using its protected
full-control constructor with `writableStackTrace = false`:

```kotlin
throw BusinessException.noStackTrace(ApiErrorCode.CONFLICT, "already bound to group {}", groupId)
```

### Custom Error Response

The default body is `ApiErrorResponse(code, name, messages)`. To customize the response shape, provide an `ApiErrorResponseFactory` bean:

```kotlin
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import zygarde.api.exception.ApiErrorResponseFactory

data class AppErrorResponse(
  val code: String,
  val name: String,
  val messages: List<String>,
  val path: String?
)

@Configuration
class AppErrorConfig {
  @Bean
  fun apiErrorResponseFactory() = ApiErrorResponseFactory { errorCode, messages, _, request ->
    AppErrorResponse(
      code = errorCode.code,
      name = errorCode.name,
      messages = messages,
      path = request?.requestURI
    )
  }
}
```

### Error Response Format

Default error response structure:

```kotlin
data class ApiErrorResponse(
  val code: String,
  val name: String,
  val messages: List<String>
)
```

Example response:

```json
{
  "code": "404",
  "name": "NOT_FOUND",
  "messages": ["Book not found: 123"]
}
```

## Validation

### Bean Validation

Use standard Java validation annotations:

```kotlin
import javax.validation.constraints.*

data class CreateBookReq(
  @field:NotBlank(message = "Title is required")
  @field:Size(min = 1, max = 200)
  val title: String,

  @field:Size(max = 1000)
  val description: String?,

  @field:NotNull
  @field:DecimalMin("0.01")
  @field:DecimalMax("9999.99")
  val price: Double,

  @field:Min(1000)
  @field:Max(9999)
  val publishedYear: Int
)
```

### Validation Error Handling

Validation errors are automatically handled:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Validation failed",
  "timestamp": 1704067200000,
  "path": "/api/books",
  "details": {
    "title": "Title is required",
    "price": "must be greater than or equal to 0.01"
  }
}
```

### Custom Validators

Create custom validation logic:

```kotlin
import javax.validation.Constraint
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [ISBNValidator::class])
annotation class ISBN(
  val message: String = "Invalid ISBN format",
  val groups: Array<KClass<*>> = [],
  val payload: Array<KClass<out Any>> = []
)

class ISBNValidator : ConstraintValidator<ISBN, String> {
  override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
    if (value == null) return true
    return value.matches(Regex("^(978|979)-\\d{1,5}-\\d{1,7}-\\d{1,7}-\\d$"))
  }
}

// Usage
data class CreateBookReq(
  @field:ISBN
  val isbn: String,
  // ... other fields
)
```

## Response Standardization

### Pagination Response

Use `PageDto` for paginated responses:

```kotlin
import zygarde.core.dto.PageDto

@GetMapping
fun getAllBooks(
  @RequestParam(defaultValue = "0") page: Int,
  @RequestParam(defaultValue = "20") size: Int
): PageDto<BookDto> {
  val pageable = PageRequest.of(page, size)
  val bookPage = dao.book.findAll(pageable)

  return PageDto(
    atPage = bookPage.number,
    totalPages = bookPage.totalPages,
    totalCount = bookPage.totalElements,
    items = bookPage.content.map { it.toBookDto() }
  )
}
```

Response format:

```json
{
  "atPage": 0,
  "totalPages": 5,
  "totalCount": 100,
  "items": [
    {"id": 1, "title": "Book 1", ...},
    {"id": 2, "title": "Book 2", ...}
  ]
}
```

### Success Response Wrapper

Optional success response wrapper:

```kotlin
data class ApiResponse<T>(
  val success: Boolean = true,
  val data: T,
  val timestamp: Long = System.currentTimeMillis()
)

@GetMapping("/{id}")
fun getBookById(@PathVariable id: Long): ApiResponse<BookDto> {
  val book = bookService.getBookById(id)
  return ApiResponse(data = book)
}
```

## Search and Filtering

### Query Parameters

```kotlin
@GetMapping("/search")
fun searchBooks(
  @RequestParam(required = false) title: String?,
  @RequestParam(required = false) authorId: Long?,
  @RequestParam(required = false) minPrice: Double?,
  @RequestParam(required = false) maxPrice: Double?,
  @RequestParam(required = false) status: Status?,
  @RequestParam(defaultValue = "0") page: Int,
  @RequestParam(defaultValue = "20") size: Int
): PageDto<BookDto> {
  return bookService.searchBooks(
    title = title,
    authorId = authorId,
    minPrice = minPrice,
    maxPrice = maxPrice,
    status = status,
    pageable = PageRequest.of(page, size)
  )
}
```

### Search Request DTO

Encapsulate search parameters:

```kotlin
import zygarde.core.dto.KeywordPagingAndSortingRequest

data class BookSearchReq(
  val title: String? = null,
  val authorId: Long? = null,
  val minPrice: Double? = null,
  val maxPrice: Double? = null,
  val status: Status? = null
) : KeywordPagingAndSortingRequest()

@PostMapping("/search")
fun searchBooks(@RequestBody request: BookSearchReq): PageDto<BookDto> {
  return bookService.searchBooks(request)
}
```

## CORS Configuration

### Global CORS

```kotlin
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class CorsConfig : WebMvcConfigurer {

  override fun addCorsMappings(registry: CorsRegistry) {
    registry.addMapping("/api/**")
      .allowedOrigins("http://localhost:3000", "https://myapp.com")
      .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
      .allowedHeaders("*")
      .allowCredentials(true)
      .maxAge(3600)
  }
}
```

### Controller-Level CORS

```kotlin
@RestController
@RequestMapping("/api/books")
@CrossOrigin(origins = ["http://localhost:3000"])
class BookController(private val bookService: BookService) : BookApi {
  // ...
}
```

## Request Tracing

### API Tracing Context

Track requests with `ApiTracingContext`:

```kotlin
import zygarde.webmvc.tracing.ApiTracingContext
import org.slf4j.LoggerFactory

@Service
class BookService(private val dao: Dao) {

  private val logger = LoggerFactory.getLogger(javaClass)

  fun getBookById(id: Long): BookDto {
    val traceId = ApiTracingContext.getTraceId()
    logger.info("[$traceId] Fetching book: $id")

    return dao.book.findById(id)
      .orElseThrow { NoSuchElementException("Book not found: $id") }
      .toBookDto()
  }
}
```

### Request Interceptor

Add trace ID to responses:

```kotlin
import org.springframework.web.servlet.HandlerInterceptor
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class TracingInterceptor : HandlerInterceptor {

  override fun preHandle(
    request: HttpServletRequest,
    response: HttpServletResponse,
    handler: Any
  ): Boolean {
    val traceId = UUID.randomUUID().toString()
    ApiTracingContext.setTraceId(traceId)
    response.addHeader("X-Trace-Id", traceId)
    return true
  }

  override fun afterCompletion(
    request: HttpServletRequest,
    response: HttpServletResponse,
    handler: Any,
    ex: Exception?
  ) {
    ApiTracingContext.clear()
  }
}
```

## Testing REST APIs

### Controller Tests

```kotlin
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.*
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every

@WebMvcTest(BookController::class)
class BookControllerTest {

  @Autowired
  lateinit var mockMvc: MockMvc

  @MockkBean
  lateinit var bookService: BookService

  @Test
  fun `should get book by id`() {
    // given
    val bookDto = BookDto(id = 1L, title = "Test Book", price = 29.99)
    every { bookService.getBookById(1L) } returns bookDto

    // when & then
    mockMvc.get("/api/books/1")
      .andExpect {
        status { isOk() }
        content { contentType(MediaType.APPLICATION_JSON) }
        jsonPath("$.id") { value(1) }
        jsonPath("$.title") { value("Test Book") }
      }
  }

  @Test
  fun `should create book`() {
    // given
    val request = CreateBookReq(title = "New Book", price = 39.99, publishedYear = 2024)
    val bookDto = BookDto(id = 1L, title = "New Book", price = 39.99)
    every { bookService.createBook(request) } returns bookDto

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

### Integration Tests

```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BookApiIntegrationTest {

  @Autowired
  lateinit var restTemplate: TestRestTemplate

  @Test
  fun `should create and retrieve book`() {
    // given
    val request = CreateBookReq(
      title = "Integration Test Book",
      price = 49.99,
      publishedYear = 2024
    )

    // when - create
    val createResponse = restTemplate.postForEntity(
      "/api/books",
      request,
      BookDto::class.java
    )

    // then - verify creation
    createResponse.statusCode shouldBe HttpStatus.OK
    val created = createResponse.body!!
    created.title shouldBe "Integration Test Book"

    // when - retrieve
    val getResponse = restTemplate.getForEntity(
      "/api/books/${created.id}",
      BookDto::class.java
    )

    // then - verify retrieval
    getResponse.statusCode shouldBe HttpStatus.OK
    getResponse.body!! shouldBe created
  }
}
```

## Using Zygarde with REST APIs

### 1. Use Generated DTOs from Zygarde

Always use Zygarde-generated DTOs at API boundaries:

```kotlin
// ✅ Use Zygarde-generated DTOs
@GetMapping("/{id}")
fun getBook(@PathVariable id: Long): BookDto {
  return dao.book.findById(id)
    .map { it.toBookDto() }  // Generated by Zygarde
    .orElseThrow { NotFoundException() }
}

// ❌ Avoid exposing entities directly
@GetMapping("/{id}")
fun getBook(@PathVariable id: Long): Book
```

### 2. Combine Zygarde Search with DTOs

Use search DSL with generated mapping:

```kotlin
@GetMapping("/search")
fun searchBooks(@RequestParam keyword: String): List<BookDto> {
  return dao.book.search {
    title() containsIgnoreCase keyword
  }.map { it.toBookDto() }
}
```

### 3. Use Zygarde-Generated API Layer (Optional)

If using Zygarde's Web DSL code generation:

```kotlin
// Define API in DSL
api("BookApi") {
  basePath = "/api/books"
  endpoints = listOf(
    endpoint {
      name = "searchBooks"
      method = GET
      path = "/search"
      params = listOf("keyword" to "String")
      returns = "List<BookDto>"
    }
  )
}

// Zygarde generates controller, service interface, and wiring
```

## Next Steps

- **[Model Mapping →](model-mapping.md)** - Create DTOs for APIs
- **[Search DSL →](search-dsl.md)** - Implement search endpoints
- **[Common Patterns →](../reference/common-patterns.md)** - API best practices
- **[Tutorials →](../tutorials/kapt-based.md)** - Complete REST API example
