# Advanced Query Patterns

Master complex querying scenarios using Zygarde's search DSL.

## Complex Conditions

### Multiple AND Conditions

```kotlin
dao.book.search {
  title() notNull()
  publishedYear() gte 2020
  price() between (10.0 to 100.0)
  author().country() eq "USA"
}
```

### Multiple OR Conditions

```kotlin
dao.book.search {
  or {
    title() contains "Kotlin"
    title() contains "Java"
    title() contains "Spring"
  }
}
```

### Nested AND/OR

```kotlin
dao.book.search {
  // (title contains "Kotlin" OR description contains "Kotlin")
  // AND published >= 2020
  // AND price <= 50
  
  or {
    title() contains "Kotlin"
    description() contains "Kotlin"
  }
  publishedYear() gte 2020
  price() lte 50.0
}
```

### Complex Boolean Logic

```kotlin
dao.product.search {
  // (category = "Electronics" AND inStock = true)
  // OR (category = "Books" AND discount > 20)
  
  or {
    and {
      category() eq "Electronics"
      inStock() eq true
    }
    and {
      category() eq "Books"
      discount() gt 20.0
    }
  }
}
```

## Range Queries

### Between

```kotlin
// Price between 10 and 100
dao.book.search {
  price() between (10.0 to 100.0)
}

// Published between 2020 and 2023
dao.book.search {
  publishedYear() between (2020 to 2023)
}

// Date range
dao.order.search {
  createdAt() between (startDate to endDate)
}
```

### Greater Than / Less Than

```kotlin
dao.book.search {
  price() gt 20.0          // price > 20
  price() gte 20.0         // price >= 20
  price() lt 100.0         // price < 100
  price() lte 100.0        // price <= 100
}
```

## String Matching

### Contains (Case-Sensitive)

```kotlin
dao.book.search {
  title() contains "Kotlin"
}
```

### Contains (Case-Insensitive)

```kotlin
dao.book.search {
  title() containsIgnoreCase "kotlin"
  description() containsIgnoreCase "spring boot"
}
```

### Starts With / Ends With

```kotlin
dao.user.search {
  email() startsWith "admin"
  email() endsWith "@company.com"
}
```

### Pattern Matching (LIKE)

```kotlin
dao.user.search {
  email() like "%@gmail.com"
  username() like "user_%"
}
```

### Multiple String Patterns

```kotlin
dao.article.search {
  or {
    title() containsIgnoreCase "kotlin"
    title() containsIgnoreCase "java"
    content() containsIgnoreCase keyword
  }
}
```

## Null Handling

### Null Checks

```kotlin
dao.book.search {
  description() isNull()
  isbn() notNull()
}
```

### Optional Criteria

```kotlin
fun searchBooks(
  title: String?,
  authorId: Long?,
  minPrice: Double?
): List<Book> {
  return dao.book.search {
    // Only add condition if parameter is not null
    title?.let { title() containsIgnoreCase it }
    authorId?.let { author().id() eq it }
    minPrice?.let { price() gte it }
  }
}
```

## Collection Operations

### In Clause

```kotlin
dao.book.search {
  category() isIn listOf("Fiction", "Science", "History")
}

dao.user.search {
  id() isIn userIds
}
```

### Not In Clause

```kotlin
dao.product.search {
  status() notIn listOf("DELETED", "ARCHIVED")
}
```

## Join Queries

### Simple Join

```kotlin
// Automatically creates join to author
dao.book.search {
  author().name() contains "Martin"
}
```

### Nested Joins

```kotlin
// Book -> Author -> Country
dao.book.search {
  author().country().name() eq "United States"
  author().country().continent() eq "North America"
}
```

### Multiple Joins

```kotlin
// Book has Publisher and Author
dao.book.search {
  author().name() contains "Robert"
  publisher().name() eq "O'Reilly"
  publisher().country() eq "USA"
}
```

### Join with Conditions

```kotlin
dao.book.search {
  // Books by active authors from USA
  author().status() eq AuthorStatus.ACTIVE
  author().country() eq "USA"
  publishedYear() gte 2020
}
```

## Sorting

### Single Field Sort

```kotlin
dao.book.search {
  title() notNull()
  orderBy(Book::publishedYear, desc = true)
}
```

### Multiple Field Sort

```kotlin
dao.book.search {
  author().country() eq "USA"
  orderBy(Book::publishedYear, desc = true)
  orderBy(Book::title, desc = false)
}
```

### Sort with Joins

```kotlin
dao.book.search {
  // Sort by author name
  orderBy { author().name() }
}
```

## Pagination

### With Spring Data Pageable

```kotlin
fun searchBooks(
  keyword: String,
  pageable: Pageable
): Page<Book> {
  val spec = dao.book.toSpecification {
    title() containsIgnoreCase keyword
  }
  return dao.book.findAll(spec, pageable)
}
```

### Manual Pagination

```kotlin
fun getBooksPage(page: Int, size: Int): Page<Book> {
  val pageable = PageRequest.of(
    page, 
    size, 
    Sort.by("publishedYear").descending()
  )
  return dao.book.findAll(pageable)
}
```

## Aggregations (Enhanced DAO)

### Count

```kotlin
// Count books by category
val fictionCount = dao.book.count {
  category() eq "Fiction"
}

// Count active users
val activeUsers = dao.user.count {
  active() eq true
  lastLoginAt() gte LocalDateTime.now().minusDays(30)
}
```

### Exists

```kotlin
val hasExpensiveBooks = dao.book.search {
  price() gt 100.0
}.isNotEmpty()

// Or with count
val exists = dao.book.count {
  price() gt 100.0
} > 0
```

## Date/Time Queries

### Date Ranges

```kotlin
dao.order.search {
  createdAt() gte LocalDateTime.now().minusDays(7)
  createdAt() lte LocalDateTime.now()
}
```

### Date Comparisons

```kotlin
val today = LocalDate.now()
val startOfMonth = today.withDayOfMonth(1)
val endOfMonth = today.withDayOfMonth(today.lengthOfMonth())

dao.order.search {
  orderDate() between (startOfMonth to endOfMonth)
}
```

### Time-Based Filtering

```kotlin
dao.user.search {
  // Users created in last 30 days
  createdAt() gte LocalDateTime.now().minusDays(30)
  
  // Users who logged in this year
  lastLoginAt() gte LocalDateTime.of(LocalDate.now().year, 1, 1, 0, 0)
}
```

## Dynamic Queries

### Search Builder Pattern

```kotlin
fun searchProducts(criteria: ProductSearchCriteria): List<Product> {
  return dao.product.search {
    // Apply filters dynamically
    criteria.category?.let { category() eq it }
    criteria.minPrice?.let { price() gte it }
    criteria.maxPrice?.let { price() lte it }
    criteria.inStock?.let { inStock() eq it }
    
    criteria.keyword?.let { keyword ->
      or {
        name() containsIgnoreCase keyword
        description() containsIgnoreCase keyword
      }
    }
    
    // Apply sorting
    when (criteria.sortBy) {
      "price" -> orderBy(Product::price, desc = criteria.sortDesc)
      "name" -> orderBy(Product::name, desc = criteria.sortDesc)
      else -> orderBy(Product::createdAt, desc = true)
    }
  }
}
```

### Conditional Joins

```kotlin
fun searchBooks(includeAuthorFilter: Boolean, authorCountry: String?): List<Book> {
  return dao.book.search {
    if (includeAuthorFilter && authorCountry != null) {
      author().country() eq authorCountry
    }
  }
}
```

## Performance Optimization

### Fetch Joins (Avoid N+1)

```kotlin
// Eagerly fetch related entities
val books = dao.book.search {
  category() eq "Programming"
  // Configure fetch join in repository or use EntityGraph
}
```

### Select Only Needed Fields

```kotlin
// Use projections for specific fields
interface BookProjection {
  val id: Long
  val title: String
  val authorName: String
}

// Define custom repository method
fun findBookProjections(): List<BookProjection> {
  // Custom JPQL or Criteria query
}
```

### Limit Results

```kotlin
fun findTopBooks(limit: Int): List<Book> {
  val pageable = PageRequest.of(0, limit, Sort.by("rating").descending())
  return dao.book.findAll(pageable).content
}
```

## Real-World Examples

### E-Commerce Product Search

```kotlin
data class ProductFilter(
  val keyword: String?,
  val categoryIds: List<Long>?,
  val minPrice: Double?,
  val maxPrice: Double?,
  val inStockOnly: Boolean,
  val tags: List<String>?
)

fun searchProducts(filter: ProductFilter): List<Product> {
  return dao.product.search {
    filter.keyword?.let { keyword ->
      or {
        name() containsIgnoreCase keyword
        description() containsIgnoreCase keyword
      }
    }
    
    filter.categoryIds?.let { 
      category().id() isIn it 
    }
    
    filter.minPrice?.let { price() gte it }
    filter.maxPrice?.let { price() lte it }
    
    if (filter.inStockOnly) {
      stock() gt 0
    }
    
    filter.tags?.forEach { tag ->
      tags() contains tag
    }
  }
}
```

### User Activity Report

```kotlin
fun getUserActivityReport(
  startDate: LocalDateTime,
  endDate: LocalDateTime,
  roles: List<String>?
): List<User> {
  return dao.user.search {
    // Active in date range
    or {
      lastLoginAt() between (startDate to endDate)
      createdAt() between (startDate to endDate)
    }
    
    // Filter by roles
    roles?.let { 
      role() isIn it 
    }
    
    // Only active users
    active() eq true
    
    // Sort by recent activity
    orderBy(User::lastLoginAt, desc = true)
  }
}
```

### Advanced Blog Search

```kotlin
fun searchArticles(
  query: String?,
  tags: List<String>?,
  authorId: Long?,
  publishedAfter: LocalDate?,
  publishedBefore: LocalDate?,
  status: ArticleStatus?
): List<Article> {
  return dao.article.search {
    query?.let { keyword ->
      or {
        title() containsIgnoreCase keyword
        content() containsIgnoreCase keyword
        tags() containsIgnoreCase keyword
      }
    }
    
    tags?.forEach { tag ->
      tags() contains tag
    }
    
    authorId?.let { author().id() eq it }
    
    publishedAfter?.let { 
      publishedAt() gte it.atStartOfDay() 
    }
    
    publishedBefore?.let { 
      publishedAt() lte it.atTime(23, 59, 59) 
    }
    
    status?.let { 
      this.status() eq it 
    } ?: run {
      // Default to published only
      this.status() eq ArticleStatus.PUBLISHED
    }
    
    orderBy(Article::publishedAt, desc = true)
  }
}
```

## Zygarde DSL Tips

### 1. Use Type-Safe Search

Zygarde provides compile-time safety - leverage it:

```kotlin
// Zygarde's type-safe DSL
dao.book.search {
  title() contains "Kotlin"
}

// No string-based queries needed
```

### 2. Reuse Search Logic with Extension Functions

Extract common Zygarde search patterns:

```kotlin
// Extension function for reusable search logic
private fun SearchScope<Book, Book>.onlyPublished() {
  status() eq BookStatus.PUBLISHED
  publishedAt() notNull()
}

fun searchPublishedBooks(keyword: String) = dao.book.search {
  onlyPublished()  // Reuse common criteria
  title() contains keyword
}
```

### 3. Handle Optional Parameters with Kotlin's let

Use Kotlin idioms with Zygarde search:

```kotlin
fun search(title: String?, category: String?) = dao.book.search {
  title?.let { title() containsIgnoreCase it }
  category?.let { category() eq it }
}
```

### 4. Optimize Join Depth

Zygarde auto-creates joins, but keep them reasonable:

```kotlin
// Good - single level join
dao.book.search {
  author().name() contains "Martin"
}

// Avoid - excessive nesting impacts performance
// book -> author -> publisher -> country -> continent
```

## Next Steps

- **[Relationships](relationships.md)** - Working with entity relationships
- **[Search DSL Reference](../guide/search-dsl.md)** - Complete DSL documentation
