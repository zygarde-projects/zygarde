# Working with Relationships

Learn how to query and manage entity relationships using Zygarde's search DSL.

## Relationship Types

### One-to-Many

```kotlin
@Entity
@ZyModel
data class Author(
  @Id @GeneratedValue
  val id: Long? = null,
  val name: String,
  
  @OneToMany(mappedBy = "author", cascade = [CascadeType.ALL])
  val books: MutableList<Book> = mutableListOf()
) : AutoLongIdEntity()

@Entity
@ZyModel
data class Book(
  @Id @GeneratedValue
  val id: Long? = null,
  val title: String,
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "author_id")
  val author: Author
) : AutoLongIdEntity()
```

### Many-to-One

```kotlin
@Entity
@ZyModel
data class Order(
  @Id @GeneratedValue
  val id: Long? = null,
  val orderNumber: String,
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "customer_id")
  val customer: Customer
) : AutoLongIdEntity()
```

### Many-to-Many

```kotlin
@Entity
@ZyModel
data class Student(
  @Id @GeneratedValue
  val id: Long? = null,
  val name: String,
  
  @ManyToMany
  @JoinTable(
    name = "student_course",
    joinColumns = [JoinColumn(name = "student_id")],
    inverseJoinColumns = [JoinColumn(name = "course_id")]
  )
  val courses: MutableSet<Course> = mutableSetOf()
) : AutoLongIdEntity()

@Entity
@ZyModel
data class Course(
  @Id @GeneratedValue
  val id: Long? = null,
  val name: String,
  
  @ManyToMany(mappedBy = "courses")
  val students: MutableSet<Student> = mutableSetOf()
) : AutoLongIdEntity()
```

### One-to-One

```kotlin
@Entity
@ZyModel
data class User(
  @Id @GeneratedValue
  val id: Long? = null,
  val username: String,
  
  @OneToOne(mappedBy = "user", cascade = [CascadeType.ALL])
  val profile: UserProfile?
) : AutoLongIdEntity()

@Entity
@ZyModel
data class UserProfile(
  @Id @GeneratedValue
  val id: Long? = null,
  val bio: String,
  
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  val user: User
) : AutoLongIdEntity()
```

## Querying Relationships

### Query via Related Entity

```kotlin
// Find all books by a specific author
dao.book.search {
  author().id() eq authorId
}

// Find books by author name
dao.book.search {
  author().name() contains "Martin"
}
```

### Query with Nested Relationships

```kotlin
@Entity
@ZyModel
data class Book(
  @Id @GeneratedValue
  val id: Long? = null,
  val title: String,
  
  @ManyToOne
  val author: Author
)

@Entity
@ZyModel
data class Author(
  @Id @GeneratedValue
  val id: Long? = null,
  val name: String,
  
  @ManyToOne
  val country: Country
)

@Entity
@ZyModel
data class Country(
  @Id @GeneratedValue
  val id: Long? = null,
  val name: String,
  val continent: String
)

// Query across multiple levels
dao.book.search {
  author().country().name() eq "United States"
  author().country().continent() eq "North America"
}
```

### Multiple Relationship Conditions

```kotlin
dao.book.search {
  // Author criteria
  author().name() contains "Robert"
  author().active() eq true
  
  // Publisher criteria  
  publisher().name() eq "O'Reilly"
  publisher().country() eq "USA"
  
  // Book criteria
  publishedYear() gte 2020
}
```

## CRUD with Relationships

### Creating with Relationships

#### Approach 1: Set Foreign Key

```kotlin
@Transactional
fun createBook(authorId: Long, title: String): Book {
  val author = dao.author.findById(authorId)
    .orElseThrow { NotFoundException("Author not found") }
  
  val book = Book(
    title = title,
    author = author
  )
  
  return dao.book.save(book)
}
```

#### Approach 2: Cascade Save (One-to-Many)

```kotlin
@Transactional
fun createAuthorWithBooks(
  authorName: String,
  bookTitles: List<String>
): Author {
  val author = Author(name = authorName)
  
  bookTitles.forEach { title ->
    val book = Book(title = title, author = author)
    author.books.add(book)
  }
  
  // Cascade will save books
  return dao.author.save(author)
}
```

#### Approach 3: Many-to-Many

```kotlin
@Transactional
fun enrollStudentInCourses(
  studentId: Long,
  courseIds: List<Long>
): Student {
  val student = dao.student.findById(studentId)
    .orElseThrow { NotFoundException("Student not found") }
  
  val courses = dao.course.findAllById(courseIds)
  student.courses.addAll(courses)
  
  return dao.student.save(student)
}
```

### Updating Relationships

#### Change Parent Reference

```kotlin
@Transactional
fun changeBookAuthor(bookId: Long, newAuthorId: Long): Book {
  val book = dao.book.findById(bookId)
    .orElseThrow { NotFoundException("Book not found") }
  
  val newAuthor = dao.author.findById(newAuthorId)
    .orElseThrow { NotFoundException("Author not found") }
  
  val updated = book.copy(author = newAuthor)
  return dao.book.save(updated)
}
```

#### Add to Collection

```kotlin
@Transactional
fun addBookToAuthor(authorId: Long, bookTitle: String): Author {
  val author = dao.author.findById(authorId)
    .orElseThrow { NotFoundException("Author not found") }
  
  val book = Book(title = bookTitle, author = author)
  author.books.add(book)
  
  return dao.author.save(author)
}
```

#### Remove from Collection

```kotlin
@Transactional
fun removeStudentFromCourse(studentId: Long, courseId: Long) {
  val student = dao.student.findById(studentId)
    .orElseThrow { NotFoundException("Student not found") }
  
  student.courses.removeIf { it.id == courseId }
  dao.student.save(student)
}
```

### Deleting with Relationships

#### Delete Child (Orphan)

```kotlin
@Transactional
fun deleteBook(bookId: Long) {
  // Simply delete the book
  // Author remains
  dao.book.deleteById(bookId)
}
```

#### Delete with Orphan Removal

```kotlin
@Entity
@ZyModel
data class Author(
  @Id @GeneratedValue
  val id: Long? = null,
  val name: String,
  
  @OneToMany(
    mappedBy = "author",
    cascade = [CascadeType.ALL],
    orphanRemoval = true  // Delete books when removed from collection
  )
  val books: MutableList<Book> = mutableListOf()
)

@Transactional
fun removeBookFromAuthor(authorId: Long, bookId: Long) {
  val author = dao.author.findById(authorId)
    .orElseThrow { NotFoundException("Author not found") }
  
  // Remove from collection - book will be deleted
  author.books.removeIf { it.id == bookId }
  dao.author.save(author)
}
```

#### Cascade Delete

```kotlin
@Transactional
fun deleteAuthor(authorId: Long) {
  // If CascadeType.ALL or CascadeType.REMOVE is set,
  // all books will be deleted too
  dao.author.deleteById(authorId)
}
```

## Complex Relationship Queries

### Filter by Collection Size

```kotlin
// Authors with more than 5 books
// This requires custom repository method
interface AuthorDao : JpaRepository<Author, Long> {
  @Query("SELECT a FROM Author a WHERE SIZE(a.books) > :count")
  fun findAuthorsWithMoreThanNBooks(@Param("count") count: Int): List<Author>
}
```

### Exists in Collection

```kotlin
// Students enrolled in a specific course
dao.student.search {
  // This requires proper join handling
  // May need custom specification
}
```

### Collection is Empty

```kotlin
// Authors with no books
// Requires custom repository method
@Query("SELECT a FROM Author a WHERE a.books IS EMPTY")
fun findAuthorsWithoutBooks(): List<Author>
```

## Fetch Strategies

### Lazy Loading (Default)

```kotlin
@ManyToOne(fetch = FetchType.LAZY)
val author: Author
```

Usage:
```kotlin
val book = dao.book.findById(id).get()
// Author not loaded yet

val authorName = book.author.name
// Author loaded on access (might cause N+1)
```

### Eager Loading

```kotlin
@ManyToOne(fetch = FetchType.EAGER)
val author: Author
```

### Fetch Join in Query

```kotlin
// Using JPQL with fetch join
interface BookDao : JpaRepository<Book, Long> {
  @Query("SELECT b FROM Book b JOIN FETCH b.author WHERE b.id = :id")
  fun findByIdWithAuthor(@Param("id") id: Long): Book?
  
  @Query("SELECT DISTINCT b FROM Book b JOIN FETCH b.author")
  fun findAllWithAuthors(): List<Book>
}
```

### Entity Graph

```kotlin
interface BookDao : JpaRepository<Book, Long> {
  @EntityGraph(attributePaths = ["author", "publisher"])
  override fun findAll(): List<Book>
  
  @EntityGraph(attributePaths = ["author"])
  fun findByTitle(title: String): List<Book>
}
```

## Avoiding N+1 Queries

### Problem: N+1 Query

```kotlin
// BAD: N+1 queries
val books = dao.book.findAll()  // 1 query
books.forEach { book ->
  println(book.author.name)  // N queries (one per book)
}
```

### Solution 1: Fetch Join

```kotlin
// Define in custom repository
@Query("SELECT DISTINCT b FROM Book b JOIN FETCH b.author")
fun findAllWithAuthors(): List<Book>

// Usage
val books = dao.book.findAllWithAuthors()  // Single query
books.forEach { book ->
  println(book.author.name)  // No additional queries
}
```

### Solution 2: Entity Graph

```kotlin
@EntityGraph(attributePaths = ["author"])
override fun findAll(): List<Book>
```

### Solution 3: Batch Fetching

```kotlin
@Entity
@BatchSize(size = 10)
data class Author(/* ... */)

// Or on collection
@OneToMany(mappedBy = "author")
@BatchSize(size = 10)
val books: List<Book>
```

## Bidirectional Relationships

### Keeping Both Sides in Sync

```kotlin
@Entity
@ZyModel
data class Author(
  @Id @GeneratedValue
  val id: Long? = null,
  val name: String,
  
  @OneToMany(mappedBy = "author", cascade = [CascadeType.ALL])
  val books: MutableList<Book> = mutableListOf()
) : AutoLongIdEntity() {
  
  fun addBook(book: Book) {
    books.add(book)
    // Ensure bidirectional consistency
    if (book.author != this) {
      book.copy(author = this)
    }
  }
  
  fun removeBook(book: Book) {
    books.remove(book)
  }
}

@Entity
@ZyModel
data class Book(
  @Id @GeneratedValue
  val id: Long? = null,
  val title: String,
  
  @ManyToOne(fetch = FetchType.LAZY)
  val author: Author
) : AutoLongIdEntity()
```

Usage:
```kotlin
@Transactional
fun createAuthorWithBooks(): Author {
  val author = Author(name = "Robert Martin")
  
  val book1 = Book(title = "Clean Code", author = author)
  val book2 = Book(title = "Clean Architecture", author = author)
  
  author.addBook(book1)
  author.addBook(book2)
  
  return dao.author.save(author)
}
```

## Real-World Examples

### Blog System

```kotlin
@Entity
@ZyModel
data class Post(
  @Id @GeneratedValue
  val id: Long? = null,
  val title: String,
  val content: String,
  
  @ManyToOne
  val author: User,
  
  @OneToMany(mappedBy = "post", cascade = [CascadeType.ALL])
  val comments: MutableList<Comment> = mutableListOf(),
  
  @ManyToMany
  @JoinTable(name = "post_tag")
  val tags: MutableSet<Tag> = mutableSetOf()
)

// Find posts by author
dao.post.search {
  author().id() eq userId
  orderBy(Post::createdAt, desc = true)
}

// Find posts by tag
dao.post.search {
  tags().name() eq "kotlin"
}

// Find posts with comments count > 10
// Requires custom repository method
```

### E-Commerce System

```kotlin
@Entity
@ZyModel
data class Order(
  @Id @GeneratedValue
  val id: Long? = null,
  val orderNumber: String,
  
  @ManyToOne
  val customer: Customer,
  
  @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL])
  val items: MutableList<OrderItem> = mutableListOf()
)

@Entity
@ZyModel
data class OrderItem(
  @Id @GeneratedValue
  val id: Long? = null,
  val quantity: Int,
  val price: Double,
  
  @ManyToOne
  val order: Order,
  
  @ManyToOne
  val product: Product
)

// Find orders by customer
dao.order.search {
  customer().id() eq customerId
  orderBy(Order::createdAt, desc = true)
}

// Find orders containing a specific product
dao.orderItem.search {
  product().id() eq productId
}
```

## Zygarde Framework Tips

### 1. Use DTOs with Zygarde Model Mapping

Combine Zygarde search with model mapping for clean API responses:

```kotlin
// Use Zygarde's model mapping DSL
data class BookDto(
  val id: Long,
  val title: String,
  val authorName: String
)

// Define mapping (see Model Mapping guide)
BookDto {
  fromAutoLongId(Book::id)
  from(Book::title)
  fromRef(Book::author) { it.name }
}

// Use in service
fun searchBooks(keyword: String): List<BookDto> {
  return dao.book.search {
    title() containsIgnoreCase keyword
  }.map { it.toBookDto() }  // Generated by Zygarde
}
```

## Next Steps

- **[Search DSL Guide](../guide/search-dsl.md)** - Complete DSL reference
- **[Model Mapping](../guide/model-mapping.md)** - DTO transformations
