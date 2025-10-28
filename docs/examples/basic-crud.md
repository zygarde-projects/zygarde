# Basic CRUD Operations

Learn how to perform Create, Read, Update, and Delete operations using Zygarde's generated DAOs.

## Entity Setup

First, define your entity with `@ZyModel`:

```kotlin
@Entity
@Table(name = "users")
@ZyModel
data class User(
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,
  
  @Column(nullable = false)
  val username: String,
  
  @Column(nullable = false)
  val email: String,
  
  val fullName: String? = null,
  
  val active: Boolean = true,
  
  @Column(name = "created_at")
  val createdAt: LocalDateTime = LocalDateTime.now()
) : AutoLongIdEntity()
```

After building, Zygarde generates `UserDao` and search DSL.

## Create Operations

### Single Entity

```kotlin
@Service
class UserService(private val dao: Dao) {
  
  @Transactional
  fun createUser(username: String, email: String): User {
    val user = User(
      username = username,
      email = email,
      fullName = null
    )
    return dao.user.save(user)
  }
}
```

### Batch Creation

```kotlin
@Transactional
fun createUsers(userRequests: List<CreateUserRequest>): List<User> {
  val users = userRequests.map { req ->
    User(
      username = req.username,
      email = req.email,
      fullName = req.fullName
    )
  }
  return dao.user.saveAll(users)
}
```

## Read Operations

### Find by ID

```kotlin
fun getUserById(id: Long): User? {
  return dao.user.findById(id).orElse(null)
}

// Or with exception
fun getUserByIdOrThrow(id: Long): User {
  return dao.user.findById(id)
    .orElseThrow { NotFoundException("User not found: $id") }
}
```

### Find All

```kotlin
fun getAllUsers(): List<User> {
  return dao.user.findAll()
}

// With pagination
fun getUsersPaged(page: Int, size: Int): Page<User> {
  val pageable = PageRequest.of(page, size, Sort.by("createdAt").descending())
  return dao.user.findAll(pageable)
}
```

### Search with Criteria

```kotlin
fun findActiveUsers(): List<User> {
  return dao.user.search {
    active() eq true
  }
}

fun findUserByEmail(email: String): User? {
  return dao.user.search {
    email() eq email
  }.firstOrNull()
}

fun searchUsersByName(keyword: String): List<User> {
  return dao.user.search {
    or {
      username() containsIgnoreCase keyword
      fullName() containsIgnoreCase keyword
    }
  }
}
```

## Update Operations

### Update Single Field

```kotlin
@Transactional
fun deactivateUser(id: Long): User? {
  val user = dao.user.findById(id).orElse(null) ?: return null
  val updated = user.copy(active = false)
  return dao.user.save(updated)
}
```

### Update Multiple Fields

```kotlin
@Transactional
fun updateUserProfile(id: Long, request: UpdateProfileRequest): User? {
  val user = dao.user.findById(id).orElse(null) ?: return null
  val updated = user.copy(
    fullName = request.fullName,
    email = request.email
  )
  return dao.user.save(updated)
}
```

### Batch Update

```kotlin
@Transactional
fun activateMultipleUsers(userIds: List<Long>) {
  val users = dao.user.findAllById(userIds)
  val activatedUsers = users.map { it.copy(active = true) }
  dao.user.saveAll(activatedUsers)
}
```

## Delete Operations

### Delete by ID

```kotlin
@Transactional
fun deleteUser(id: Long) {
  dao.user.deleteById(id)
}

// With existence check
@Transactional
fun deleteUserIfExists(id: Long): Boolean {
  if (dao.user.existsById(id)) {
    dao.user.deleteById(id)
    return true
  }
  return false
}
```

### Delete by Entity

```kotlin
@Transactional
fun deleteUser(user: User) {
  dao.user.delete(user)
}
```

### Delete with Criteria (Enhanced DAO)

If using `ZygardeEnhancedDao`:

```kotlin
@Transactional
fun deleteInactiveUsers() {
  dao.user.remove {
    active() eq false
  }
}

@Transactional
fun deleteUsersByDomain(domain: String) {
  dao.user.remove {
    email() like "%@$domain"
  }
}
```

### Batch Delete

```kotlin
@Transactional
fun deleteMultipleUsers(userIds: List<Long>) {
  dao.user.deleteAllById(userIds)
}
```

## Checking Existence

### By ID

```kotlin
fun userExists(id: Long): Boolean {
  return dao.user.existsById(id)
}
```

### By Criteria

```kotlin
fun emailExists(email: String): Boolean {
  return dao.user.search {
    email() eq email
  }.isNotEmpty()
}

fun usernameAvailable(username: String): Boolean {
  return dao.user.search {
    username() eq username
  }.isEmpty()
}
```

## Counting

### Count All

```kotlin
fun getTotalUsers(): Long {
  return dao.user.count()
}
```

### Count with Criteria (Enhanced DAO)

```kotlin
fun countActiveUsers(): Int {
  return dao.user.count {
    active() eq true
  }
}

fun countUsersByDomain(domain: String): Int {
  return dao.user.count {
    email() like "%@$domain"
  }
}
```

## Complete Service Example

```kotlin
@Service
class UserService(private val dao: Dao) {
  
  @Transactional
  fun createUser(request: CreateUserRequest): User {
    // Check if email exists
    val emailExists = dao.user.search { 
      email() eq request.email 
    }.isNotEmpty()
    
    if (emailExists) {
      throw BusinessException("Email already registered")
    }
    
    val user = User(
      username = request.username,
      email = request.email,
      fullName = request.fullName
    )
    return dao.user.save(user)
  }
  
  fun getUserById(id: Long): User {
    return dao.user.findById(id)
      .orElseThrow { NotFoundException("User not found") }
  }
  
  fun searchUsers(query: String?, activeOnly: Boolean): List<User> {
    return dao.user.search {
      if (activeOnly) {
        active() eq true
      }
      
      query?.let { keyword ->
        or {
          username() containsIgnoreCase keyword
          fullName() containsIgnoreCase keyword
          email() containsIgnoreCase keyword
        }
      }
    }
  }
  
  @Transactional
  fun updateUser(id: Long, request: UpdateUserRequest): User {
    val user = getUserById(id)
    val updated = user.copy(
      fullName = request.fullName ?: user.fullName,
      email = request.email ?: user.email
    )
    return dao.user.save(updated)
  }
  
  @Transactional
  fun deactivateUser(id: Long): User {
    val user = getUserById(id)
    val deactivated = user.copy(active = false)
    return dao.user.save(deactivated)
  }
  
  @Transactional
  fun deleteUser(id: Long) {
    if (!dao.user.existsById(id)) {
      throw NotFoundException("User not found")
    }
    dao.user.deleteById(id)
  }
}
```

## Zygarde-Specific Tips

### Leverage Search DSL

Use Zygarde's type-safe search DSL instead of manual criteria building:

```kotlin
// Zygarde way - readable and type-safe
dao.user.search {
  active() eq true
  createdAt() gte LocalDateTime.now().minusDays(7)
}

// Avoid verbose Criteria API or string-based queries
```

### Validate Using Search DSL

Use Zygarde search for validation checks:

```kotlin
@Transactional
fun createUser(request: CreateUserRequest): User {
  // Use search DSL for uniqueness check
  if (dao.user.search { email() eq request.email }.isNotEmpty()) {
    throw BusinessException("Email already exists")
  }
  
  return dao.user.save(User(/* ... */))
}
```

## Next Steps

- **[Advanced Queries](advanced-queries.md)** - Complex search patterns
- **[Relationships](relationships.md)** - Working with entity relationships
- **[Search DSL Guide](../guide/search-dsl.md)** - Complete DSL reference
