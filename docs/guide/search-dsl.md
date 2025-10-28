# Search DSL

Zygarde's type-safe search DSL provides a fluent, expressive way to query entities without dealing with JPA Criteria API complexity.

## Overview

The search DSL automatically generates type-safe extension functions for entity fields, enabling intuitive query construction:

```kotlin
val books = bookDao.search {
  title() contains "Kotlin"
  author().country() eq "USA"
  publishedYear() gte 2020
}
```

## Basic Queries

### Equality

```kotlin
// Find books with exact title
bookDao.search {
  title() eq "Effective Kotlin"
}

// Not equal
bookDao.search {
  status() notEq Status.ARCHIVED
}
```

### Null Checks

```kotlin
// Find books with description
bookDao.search {
  description() notNull()
}

// Find books without description
bookDao.search {
  description() isNull()
}
```

### Membership

```kotlin
// Find books with specific statuses
bookDao.search {
  status() inList listOf(Status.PUBLISHED, Status.BESTSELLER)
}

// Exclude statuses
bookDao.search {
  status() notInList listOf(Status.ARCHIVED, Status.DELETED)
}
```

## String Operations

### Contains

```kotlin
// Case-sensitive contains
bookDao.search {
  title() contains "Kotlin"
}

// Case-insensitive contains
bookDao.search {
  title() containsIgnoreCase "kotlin"
}
```

### Starts With / Ends With

```kotlin
// Starts with
bookDao.search {
  isbn() startsWith "978-"
}

// Ends with
bookDao.search {
  title() endsWith "Guide"
}
```

### Pattern Matching

```kotlin
// SQL LIKE pattern
bookDao.search {
  title() like "%Kotlin%Programming%"
}

// NOT LIKE
bookDao.search {
  title() notLike "%Beginner%"
}
```

## Comparable Operations

For numeric, date, and other comparable types:

### Comparisons

```kotlin
// Greater than
bookDao.search {
  price() gt 50.0
}

// Greater than or equal
bookDao.search {
  publishedYear() gte 2020
}

// Less than
bookDao.search {
  pageCount() lt 300
}

// Less than or equal
bookDao.search {
  rating() lte 3.5
}
```

### Range Queries

```kotlin
// Between (inclusive)
bookDao.search {
  publishedYear() between (2018 to 2023)
}

// Date ranges
bookDao.search {
  createdAt() between (startDate to endDate)
}
```

## Boolean Logic

### AND Conditions

By default, all conditions are combined with AND:

```kotlin
bookDao.search {
  title() contains "Kotlin"
  status() eq Status.PUBLISHED
  price() lte 50.0
}
// Equivalent to: title CONTAINS 'Kotlin' AND status = 'PUBLISHED' AND price <= 50.0
```

Explicit AND blocks:

```kotlin
bookDao.search {
  and {
    title() contains "Kotlin"
    author().country() eq "USA"
  }
}
```

### OR Conditions

```kotlin
bookDao.search {
  or {
    title() contains "Kotlin"
    title() contains "Java"
    title() contains "Scala"
  }
}
```

### Combining AND/OR

```kotlin
bookDao.search {
  status() eq Status.PUBLISHED
  and {
    or {
      title() contains "Kotlin"
      description() contains "Kotlin"
    }
    price() lte 50.0
  }
}
// (status = 'PUBLISHED') AND ((title CONTAINS 'Kotlin' OR description CONTAINS 'Kotlin') AND price <= 50.0)
```

### Negation

```kotlin
bookDao.search {
  not {
    status() eq Status.ARCHIVED
  }
}
```

## Relationship Traversal

### Simple Relationships

Navigate relationships naturally:

```kotlin
// Find books by author country
bookDao.search {
  author().country() eq "USA"
}

// Nested relationships
bookDao.search {
  author().publisher().city() eq "New York"
}
```

### Multiple Conditions on Relationships

```kotlin
bookDao.search {
  author().country() eq "USA"
  author().birthYear() gte 1970
  author().status() eq AuthorStatus.ACTIVE
}
```

### Collection Relationships

```kotlin
// Books with any review rating >= 4
bookDao.search {
  reviews().rating() gte 4.0
}

// Books in specific categories
bookDao.search {
  categories().name() eq "Programming"
}
```

## Advanced Features

### Range Overlap Detection

For temporal or numeric range queries:

```kotlin
import zygarde.core.dto.SearchRangeOverlap

// Find events overlapping with a date range
eventDao.search {
  SearchRangeOverlap(
    rangeStart = queryStartDate,
    rangeEnd = queryEndDate,
    targetStart = Event::startDate,
    targetEnd = Event::endDate
  ).applyTo(this)
}
```

### Date Range Queries

```kotlin
import zygarde.core.dto.SearchDateRange
import java.time.LocalDate

bookDao.search {
  val dateRange = SearchDateRange(
    start = LocalDate.of(2020, 1, 1),
    end = LocalDate.of(2023, 12, 31)
  )

  createdAt() between (dateRange.start!! to dateRange.end!!)
}
```

### Keyword Search

Multi-field keyword search:

```kotlin
import zygarde.core.dto.SearchKeyword

fun searchBooks(keyword: String?): List<Book> {
  if (keyword.isNullOrBlank()) return bookDao.findAll()

  return bookDao.search {
    or {
      title() containsIgnoreCase keyword
      description() containsIgnoreCase keyword
      author().name() containsIgnoreCase keyword
    }
  }
}
```

## Pagination with Search DSL

### Creating Specifications

Convert search DSL to Spring Data `Specification`:

```kotlin
import zygarde.data.jpa.search.EnhancedSearchImpl

val spec = EnhancedSearchImpl<Book>().apply {
  title() contains "Kotlin"
  status() eq Status.PUBLISHED
}.toSpecification()

val pageable = PageRequest.of(0, 20)
val page = bookDao.findAll(spec, pageable)
```

### Reusable Specifications

```kotlin
object BookSpecs {

  fun activeBooks(): Specification<Book> = EnhancedSearchImpl<Book>().apply {
    status() eq Status.PUBLISHED
  }.toSpecification()

  fun byAuthorCountry(country: String): Specification<Book> =
    EnhancedSearchImpl<Book>().apply {
      author().country() eq country
    }.toSpecification()

  fun recentBooks(year: Int): Specification<Book> =
    EnhancedSearchImpl<Book>().apply {
      publishedYear() gte year
    }.toSpecification()
}

// Combine specifications
val spec = BookSpecs.activeBooks()
  .and(BookSpecs.byAuthorCountry("USA"))
  .and(BookSpecs.recentBooks(2020))

val books = bookDao.findAll(spec)
```

## Custom Actions

### Extending the DSL

Create custom search actions for domain-specific operations:

```kotlin
import zygarde.data.jpa.search.ConditionAction
import javax.persistence.criteria.*

class CustomStringAction<ROOT, CURRENT>(
  root: ROOT,
  current: CURRENT,
  path: Path<String>,
  predicates: MutableList<Predicate>,
  cb: CriteriaBuilder
) : ConditionAction<ROOT, CURRENT, String>(root, current, path, predicates, cb) {

  infix fun matchesPattern(pattern: String): ROOT {
    predicates.add(cb.like(path as Expression<String>, pattern))
    return root
  }

  infix fun lengthGreaterThan(length: Int): ROOT {
    predicates.add(cb.greaterThan(cb.length(path as Expression<String>), length))
    return root
  }
}
```

### Using Custom Actions

```kotlin
// In your generated search DSL
fun SearchScope<Book, Book>.customTitle(): CustomStringAction<Book, Book, String> {
  return CustomStringAction(
    root = this as Book,
    current = this,
    path = /* field path */,
    predicates = /* predicates list */,
    cb = /* criteria builder */
  )
}

// Usage
bookDao.search {
  customTitle() matchesPattern "%[A-Z]%"
  customTitle() lengthGreaterThan 10
}
```

## Performance Considerations

### Indexed Fields

Ensure fields used in queries are indexed:

```kotlin
@Entity
@Table(
  name = "books",
  indexes = [
    Index(name = "idx_title", columnList = "title"),
    Index(name = "idx_status", columnList = "status"),
    Index(name = "idx_published_year", columnList = "published_year")
  ]
)
class Book(/* ... */) : AutoLongIdEntity()
```

### Avoid N+1 Queries

Use fetch joins for relationships:

```kotlin
@Query("SELECT DISTINCT b FROM Book b JOIN FETCH b.author WHERE ...")
fun findWithAuthor(...): List<Book>
```

Or use `@EntityGraph`:

```kotlin
@EntityGraph(attributePaths = ["author", "publisher"])
fun findByStatus(status: Status): List<Book>
```

### Limit Result Sets

Always paginate large result sets:

```kotlin
val spec = EnhancedSearchImpl<Book>().apply {
  status() eq Status.PUBLISHED
}.toSpecification()

val pageable = PageRequest.of(0, 100) // Limit to 100 results
val page = bookDao.findAll(spec, pageable)
```

## Common Patterns

### Dynamic Filters

```kotlin
fun searchBooks(
  title: String?,
  authorId: Long?,
  minPrice: Double?,
  maxPrice: Double?,
  status: Status?
): List<Book> = bookDao.search {

  title?.let { title() containsIgnoreCase it }
  authorId?.let { author().id() eq it }
  minPrice?.let { price() gte it }
  maxPrice?.let { price() lte it }
  status?.let { status() eq it }
}
```

### Sorting

```kotlin
val spec = EnhancedSearchImpl<Book>().apply {
  status() eq Status.PUBLISHED
}.toSpecification()

val sort = Sort.by(
  Sort.Order.desc("publishedYear"),
  Sort.Order.asc("title")
)
val pageable = PageRequest.of(0, 20, sort)

val page = bookDao.findAll(spec, pageable)
```

### Count Queries

```kotlin
val count = bookDao.count {
  status() eq Status.PUBLISHED
  publishedYear() gte 2020
}

println("Found $count published books since 2020")
```

### Existence Checks

```kotlin
val hasKotlinBooks = bookDao.exists {
  title() containsIgnoreCase "Kotlin"
  status() eq Status.PUBLISHED
}

if (hasKotlinBooks) {
  println("We have Kotlin books!")
}
```

## Troubleshooting

### Type Mismatch Errors

**Problem**: Compiler error on field comparisons

**Solution**: Ensure field types match comparison values:
```kotlin
// Wrong: comparing Long to Int
book().id() eq 1

// Correct: use Long
book().id() eq 1L
```

### Lazy Initialization Exceptions

**Problem**: `LazyInitializationException` when accessing relationships

**Solution**: Use fetch joins or `@Transactional`:
```kotlin
@Transactional(readOnly = true)
fun getBookWithAuthor(id: Long): Book {
  val book = bookDao.findById(id).orElseThrow()
  book.author.name // Access within transaction
  return book
}
```

### Query Performance Issues

**Problem**: Slow queries

**Solutions**:
1. Add database indexes on queried fields
2. Use projections instead of full entities
3. Implement pagination
4. Profile queries with `show-sql: true`

## Next Steps

- **[JPA Extensions →](jpa-extensions.md)** - Advanced DAO features
- **[Model Mapping →](model-mapping.md)** - Convert entities to DTOs
- **[Common Patterns →](../reference/common-patterns.md)** - Best practices
- **[Tutorials →](../tutorials/kapt-based.md)** - Complete examples
