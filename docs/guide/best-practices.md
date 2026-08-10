# Best Practices

This guide covers production-tested patterns for structuring and developing applications with Zygarde. Following these conventions will help you maintain clean architecture, leverage code generation effectively, and keep your codebase scalable.

## Project Structure

### Multi-Module Organization

A well-organized Zygarde project separates concerns across multiple modules. The recommended layout groups modules by purpose:

```
my-app/
├── settings.gradle.kts
├── build.gradle.kts
├── bom/                              # Bill of Materials for dependency versions
│   └── build.gradle.kts
├── modules/
│   ├── my-core/                      # Entities, enums, shared constants
│   │   └── build.gradle.kts
│   ├── my-model-base/                # Base model classes, API model constants
│   │   └── build.gradle.kts
│   ├── my-api/                       # Spring Boot application, service implementations
│   │   └── build.gradle.kts
│   ├── codegen/
│   │   ├── my-core-codegen/          # KAPT: generates DAOs, DTOs, entity search, model mapping
│   │   ├── my-api-spec-codegen/      # KAPT: generates API interfaces, controllers, service interfaces
│   │   └── my-model-codegen/         # DSL: model mapping codegen specs
│   ├── codegen-generated/
│   │   ├── my-generated-jpa/         # Generated DAOs and entity search extensions
│   │   ├── my-generated-dto/         # Generated DTOs (from KAPT)
│   │   ├── my-generated-model-mapping/   # Generated DtoBuilders and applyFrom extensions
│   │   ├── my-generated-api-interface/   # Generated API interfaces
│   │   ├── my-generated-api-controller/  # Generated controllers
│   │   ├── my-generated-service-interface/ # Generated service interfaces
│   │   └── my-generated-api-interface-feign/ # Generated Feign clients (optional)
│   └── test-support/                 # Shared test utilities
│       └── build.gradle.kts
```

**Key principles:**

- **Entity module** (`my-core`) contains JPA entities, enums, and shared types. It does NOT run KAPT itself.
- **Codegen modules** (`codegen/`) contain KAPT processors and DSL specs. They read entities and write generated code to `codegen-generated/` modules.
- **Generated modules** (`codegen-generated/`) contain only generated source code. They can be safely deleted and regenerated.
- **Application module** (`my-api`) depends on generated modules and contains hand-written service implementations.

### Auto-Discovery with registerModules

Use Zygarde's `registerModules()` pattern in `settings.gradle.kts` for automatic module discovery:

```kotlin
rootProject.name = "my-app"

fun registerModules(path: String) {
  File(rootProject.projectDir, path)
    .listFiles { f -> f.isDirectory && File(f, "build.gradle.kts").exists() }
    ?.forEach { f ->
      include(f.name)
      project(":${f.name}").projectDir = f
      project(":${f.name}").name = f.name
    }
}

include("bom")
registerModules("modules")

// Skip codegen modules in CI (they only need to run locally)
if (System.getenv("CI") == null) {
  registerModules("modules/codegen")
}

registerModules("modules/codegen-generated")
```

This approach lets you add new modules by simply creating a directory with a `build.gradle.kts` file.

## Dependency Management

### BOM Pattern

Centralize all dependency versions in a Bill of Materials module:

```kotlin
// bom/build.gradle.kts
apply(plugin = "java-platform")

val zygardeVersion = extra["zygardeVersion"]

dependencies {
  constraints {
    // Zygarde runtime
    "api"("io.github.zygarde-projects:zygarde-core:$zygardeVersion")
    "api"("io.github.zygarde-projects:zygarde-jpa:$zygardeVersion")
    "api"("io.github.zygarde-projects:zygarde-jpa-envers:$zygardeVersion")
    "api"("io.github.zygarde-projects:zygarde-di:$zygardeVersion")
    "api"("io.github.zygarde-projects:zygarde-jackson:$zygardeVersion")
    "api"("io.github.zygarde-projects:zygarde-model-mapping:$zygardeVersion")
    "api"("io.github.zygarde-projects:zygarde-webmvc:$zygardeVersion")

    // Zygarde codegen (KAPT)
    "api"("io.github.zygarde-projects:zygarde-jpa-codegen:$zygardeVersion")
    "api"("io.github.zygarde-projects:zygarde-model-mapping-codegen:$zygardeVersion")
    "api"("io.github.zygarde-projects:zygarde-webmvc-codegen:$zygardeVersion")

    // Zygarde DSL codegen
    "api"("io.github.zygarde-projects:zygarde-model-mapping-codegen-dsl:$zygardeVersion")

    // Zygarde test
    "api"("io.github.zygarde-projects:zygarde-test-error-handling:$zygardeVersion")
    "api"("io.github.zygarde-projects:zygarde-test-feign:$zygardeVersion")

    // Other dependencies
    "api"("io.kotest:kotest-assertions-core-jvm:5.8.0")
    "api"("io.mockk:mockk:1.13.8")
  }
}
```

Then reference the BOM in subprojects:

```kotlin
// In root build.gradle.kts
subprojects {
  dependencies {
    api(platform(project(":bom")))
  }
}
```

This means individual modules can declare dependencies without specifying versions:

```kotlin
// In any submodule's build.gradle.kts
dependencies {
  api("io.github.zygarde-projects:zygarde-jpa")  // Version comes from BOM
}
```

### Root Build Configuration

A typical root `build.gradle.kts` defines shared configuration:

```kotlin
plugins {
  id("org.jlleitschuh.gradle.ktlint") version "10.2.0"
  id("org.springframework.boot") version "2.7.18"
  id("io.spring.dependency-management") version "1.1.3"
  kotlin("jvm") version "1.9.25"
  kotlin("plugin.spring") version "1.9.25" apply false
  kotlin("plugin.jpa") version "1.9.25" apply false
  kotlin("plugin.allopen") version "1.9.25" apply false
  kotlin("kapt") version "1.9.25" apply false
}

allprojects {
  group = "com.example"
  repositories {
    mavenCentral()
    maven("https://jitpack.io")
  }
  extra["zygardeVersion"] = "2.1.2"
}

val kaptProjects = listOf("my-core-codegen", "my-model-base", "my-api-spec-codegen")
val bootableProjects = listOf("my-api")

subprojects {
  apply(plugin = "kotlin")
  if (kaptProjects.contains(name)) apply(plugin = "kotlin-kapt")
  apply(plugin = "kotlin-jpa")
  apply(plugin = "kotlin-spring")
  apply(plugin = "kotlin-allopen")
  apply(plugin = "org.springframework.boot")
  apply(plugin = "io.spring.dependency-management")

  dependencies {
    api(platform(project(":bom")))
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    testImplementation("io.kotest:kotest-assertions-core-jvm")
  }

  tasks.withType<Test> { useJUnitPlatform() }
  tasks.getByName("bootJar").enabled = bootableProjects.contains(name)
  tasks.getByName("jar").enabled = !bootableProjects.contains(name)
}
```

## Entity Design

### Choosing Base Classes

Zygarde provides entity base classes for common ID strategies:

```kotlin
// Auto-increment Int ID (good for most entities)
@Entity
@ZyModel
class Department(
  @Column var name: String = "",
) : AutoIntIdEntity()

// Auto-increment Long ID (for entities that may grow very large)
@Entity
@ZyModel
class AuditLog(
  @Column var action: String = "",
) : AutoLongIdEntity()

// Audited entity with Envers tracking (createdAt, createdBy, etc.)
@Entity
@Audited
@AuditOverride(forClass = AuditedAutoIntIdEntity::class)
@EntityListeners(AuditingEntityListener::class)
@ZyModel
class Employee(
  @Column var name: String = "",
  @Column var email: String = "",
) : AuditedAutoIntIdEntity()
```

### Entity Conventions

Follow these conventions for entity definitions:

```kotlin
@ZyModel
@Audited
@AuditOverride(forClass = AuditedAutoIntIdEntity::class)
@Entity
@EntityListeners(AuditingEntityListener::class)
@Table(name = "employee")
class Employee(
  @Comment("Employee name")
  @Column
  var name: String = "",

  @Comment("Email address")
  @Column
  var email: String = "",

  @Comment("Status")
  @Enumerated(EnumType.STRING)
  @Column(length = 16)
  var status: EmployeeStatus = EmployeeStatus.ACTIVE,

  @Comment("Department")
  @ManyToOne(targetEntity = Department::class, fetch = FetchType.LAZY)
  open var department: Department,

  @Comment("Sort order")
  @Column(name = "sort_idx")
  var sort: Int? = 0,
) : AuditedAutoIntIdEntity()
```

**Tips:**

- Use `@Comment` from Hibernate for database column comments
- Always use `FetchType.LAZY` for `@ManyToOne` relationships
- Use `@Enumerated(EnumType.STRING)` for enums (never `ORDINAL`)
- Set reasonable `@Column(length = ...)` limits for string fields
- Use `open` modifier on relationship fields for Hibernate proxy support

### allOpen Configuration

Entities must be `open` for Hibernate proxying. Configure the `allOpen` plugin in entity modules:

```kotlin
// In the entity module's build.gradle.kts
allOpen {
  annotation("javax.persistence.Entity")
  annotation("javax.persistence.MappedSuperclass")
  annotation("javax.persistence.Embeddable")
}
```

## KAPT Configuration

### Core Codegen Module

The core codegen module runs KAPT to generate DAOs, search extensions, DTOs, and model mappings:

```kotlin
// my-core-codegen/build.gradle.kts
dependencies {
  api(project(":my-core"))
  api("io.github.zygarde-projects:zygarde-jpa")
  api("io.github.zygarde-projects:zygarde-jpa-envers")
  api("io.github.zygarde-projects:zygarde-model-mapping")
  kapt(platform(project(":bom")))
  kapt("io.github.zygarde-projects:zygarde-jpa-codegen")
  kapt("io.github.zygarde-projects:zygarde-model-mapping-codegen")
}

// Share source from the entity module so KAPT can see entities
sourceSets {
  main {
    java {
      srcDirs("src/main/kotlin", project(":my-core").file("src/main/kotlin").absolutePath)
    }
  }
}

kapt {
  arguments {
    arg("zygarde.codegen.base.package", "com.example.codegen")
    arg("zygarde.codegen.dao.inherit", "zygarde.data.jpa.dao.ZygardeEnhancedDao")
    arg("zygarde.model_mapping.dto.write_to",
      project(":my-generated-dto").file("src/main/kotlin").absolutePath)
    arg("zygarde.model_mapping.extension.write_to",
      project(":my-generated-model-mapping").file("src/main/kotlin").absolutePath)
    arg("zygarde.codegen.jpa.dao.generate_to",
      project(":my-generated-jpa").file("src/main/kotlin").absolutePath)
    arg("zygarde.codegen.jpa.entity_field.generate_to",
      project(":my-generated-jpa").file("src/main/kotlin").absolutePath)
  }
}

// Clean generated folders before each KAPT run
tasks.withType(org.jetbrains.kotlin.gradle.tasks.Kapt::class.java).all {
  doFirst {
    project(":my-generated-dto").file("src/main/kotlin").deleteRecursively()
    project(":my-generated-model-mapping").file("src/main/kotlin").deleteRecursively()
    project(":my-generated-jpa").file("src/main/kotlin").deleteRecursively()
  }
}

// Disable test KAPT (not needed)
tasks.withType<org.jetbrains.kotlin.gradle.internal.KaptTask>()
  .matching { it.name == "kaptTestKotlin" }
  .configureEach { enabled = false }
```

### API Spec Codegen Module

A separate KAPT module generates the API layer (interfaces, controllers, service interfaces):

```kotlin
// my-api-spec-codegen/build.gradle.kts
dependencies {
  implementation(project(":my-model-all"))
  kapt("io.github.zygarde-projects:zygarde-webmvc-codegen")
}

val apiInterfaceDir = project(":my-generated-api-interface").file("src/main/kotlin")
val controllerDir = project(":my-generated-api-controller").file("src/main/kotlin")
val serviceDir = project(":my-generated-service-interface").file("src/main/kotlin")

kapt {
  val apiGenFile = File.createTempFile("api-gen", ".json")
  apiGenFile.writeText(
    groovy.json.JsonOutput.toJson(
      mapOf(
        "generateApiInterfacesTo" to apiInterfaceDir.absolutePath,
        "generateControllersTo" to controllerDir.absolutePath,
        "generateServiceInterfacesTo" to serviceDir.absolutePath
      )
    )
  )
  arguments {
    arg("zygarde.codegen.base.package", "com.example.codegen")
    arg("zygarde.webmvc_codegen.api_config_json", apiGenFile.absolutePath)
  }
}

// Clean before regeneration
tasks.withType(org.jetbrains.kotlin.gradle.tasks.Kapt::class.java).all {
  doFirst {
    apiInterfaceDir.deleteRecursively()
    controllerDir.deleteRecursively()
    serviceDir.deleteRecursively()
  }
}
```

### Disabling KAPT in CI

Since generated code is committed to version control, skip KAPT in CI to speed up builds:

```kotlin
// In settings.gradle.kts
if (System.getenv("CI") == null) {
  registerModules("modules/codegen")
}

// Or in the codegen module's build.gradle.kts
if (System.getenv("CI") != null) {
  tasks.withType(org.jetbrains.kotlin.gradle.tasks.Kapt::class.java).all {
    enabled = false
  }
}
```

## Model Mapping DSL

### Structure with CodegenDtoSimple

The recommended pattern for DSL-based model mapping uses an `enum class` implementing `CodegenDtoSimple` to define DTO names:

```kotlin
class EmployeeModelSpec : ModelMappingCodegenSpec({
  Models.EmployeeDto {
    fromAutoIntId(Employee::id)
    from(
      Employee::name,
      Employee::email,
      Employee::status,
    )
  }

  Models.EmployeeDetailDto {
    fromAutoIntId(Employee::id)
    from(
      Employee::name,
      Employee::email,
      Employee::status,
    )
    fromRef("department", DepartmentModelSpec.Models.DepartmentDto)
    fromExtra(
      EmployeeModelSpec::hireDate,
      EmployeeModelSpec::performanceScore,
    )
  }

  Models.CreateEmployeeReq {
    applyTo(
      Employee::name,
      Employee::email,
    )
    field<Int>("departmentId") {
      comment = "Department ID"
    }
  }

  Models.UpdateEmployeeReq {
    applyTo(
      Employee::name,
      Employee::email,
      Employee::status,
    )
  }

  Models.SearchEmployeeReq {
    fieldNullable(
      Employee::name,
      Employee::status,
    )
  }
}) {
  enum class Models : CodegenDtoSimple {
    EmployeeDto,
    EmployeeDetailDto,
    CreateEmployeeReq,
    UpdateEmployeeReq,
    SearchEmployeeReq {
      override fun superClass() = PagingAndSortingRequest::class
    },
  }

  // Extra fields for EmployeeDetailDto
  lateinit var hireDate: LocalDate
  var performanceScore: Double = 0.0
}
```

**Key patterns:**

- **`enum class Models : CodegenDtoSimple`** - Defines DTO class names. Using an enum prevents typos and enables IDE autocomplete.
- **`superClass()` override** - Makes a DTO extend a base class (e.g., `PagingAndSortingRequest` for search requests).
- **Extra fields as class properties** - Define properties on the spec class itself, then reference them with `fromExtra()`.

### Document sortable fields for search requests

For a generated DTO that extends `PagingAndSortingRequest`, declare the fields exposed by Swagger in the DTO mapping:

```kotlin
Models.SearchEmployeeReq {
  fieldNullable(Employee::name)
  sortableFields(Employee::id, Employee::name)
  sortableField(Employee::department, Department::name)
}
```

The generated OpenAPI request schema exposes `id`, `name`, and `department.name` as the allowed values for `sorts[].field`.
The runtime type remains `String`; this metadata documents the supported fields without changing request deserialization.
Nested paths must traverse to-one values. Collection relations such as `OneToMany` are rejected because they do not define an unambiguous root-entity order.

### Mapping Entity Fields to DTOs

```kotlin
// Map auto-increment ID
fromAutoIntId(Employee::id)    // for Int IDs
fromAutoLongId(Employee::id)   // for Long IDs

// Map single field
from(Employee::name)

// Map multiple fields at once (preferred)
from(
  Employee::name,
  Employee::email,
  Employee::status,
)
```

### Mapping Relationships

```kotlin
// Reference to another DTO (many-to-one)
fromRef("department", DepartmentModelSpec.Models.DepartmentDto)

// Nullable reference
fromRef("department", DepartmentModelSpec.Models.DepartmentDto, nullable = true) {
  comment = "Employee's department"
}

// Collection of referenced DTOs (one-to-many)
fromRefCollection("employees", EmployeeModelSpec.Models.EmployeeDto)

// Nullable collection reference
fromRefCollection(
  fieldName = "employees",
  dtoRef = EmployeeModelSpec.Models.EmployeeDto,
  nullable = true
)
```

### Extra/Computed Fields

Use `fromExtra()` for fields that are computed or come from sources other than the entity:

```kotlin
class BookModelSpec : ModelMappingCodegenSpec({
  Models.BookDetailDto {
    fromAutoIntId(Book::id)
    from(Book::title, Book::price)
    fromExtra(
      BookModelSpec::averageRating,
      BookModelSpec::reviewCount,
    )
    // Inline extra with explicit type
    fromExtra<String>("publisherName")
  }
}) {
  enum class Models : CodegenDtoSimple {
    BookDetailDto,
  }

  var averageRating: Double = 0.0
  var reviewCount: Int = 0
}
```

### Request DTO Fields

For request DTOs, use `applyTo()`, `field()`, and related methods:

```kotlin
Models.CreateBookReq {
  // Fields that map directly to entity properties
  applyTo(
    Book::title,
    Book::description,
    Book::price,
  )

  // Extra field not on the entity
  field<Int>("authorId") {
    comment = "Author ID"
  }

  // Nullable field
  fieldNullable(
    Book::isbn,
  )

  // Reference to another DTO in the request
  fieldRef("publisher", PublisherModelSpec.Models.PublisherDto)

  // Collection of references
  fieldRefCollection("tags", TagModelSpec.Models.TagDto)
}
```

### Grouping Shared Mappings

Use `group()` when multiple DTOs share the same field mappings:

```kotlin
// Both DTOs will get the same name and status fields
group(Models.EmployeeListDto, Models.EmployeeSearchResultDto) {
  fromAutoIntId(Employee::id)
  from(
    Employee::name,
    Employee::status,
  )
}

// Individual DTOs can still have their own additional fields
Models.EmployeeListDto {
  fromExtra(EmployeeModelSpec::departmentName)
}
```

### Nested Extra Classes

When different DTOs need different extra fields, define nested classes:

```kotlin
class EmployeeModelSpec : ModelMappingCodegenSpec({
  Models.ManagerDto {
    fromAutoIntId(Employee::id)
    from(Employee::name)
    fromExtra(ManagerExtra::teamSize)
  }

  Models.ContractorDto {
    fromAutoIntId(Employee::id)
    from(Employee::name)
    fromExtra(ContractorExtra::contractEndDate)
  }
}) {
  enum class Models : CodegenDtoSimple {
    ManagerDto,
    ContractorDto,
  }

  class ManagerExtra {
    var teamSize: Int = 0
  }

  class ContractorExtra {
    lateinit var contractEndDate: LocalDate
  }
}
```

### DSL Execution Configuration

Configure the DSL model codegen module:

```kotlin
// my-model-codegen/build.gradle.kts
apply(plugin = "application")

dependencies {
  implementation(project(":my-core"))
  implementation(project(":my-generated-dto"))  // for cross-referencing existing DTOs
  implementation("io.github.zygarde-projects:zygarde-model-mapping")
  implementation("io.github.zygarde-projects:zygarde-model-mapping-codegen-dsl")
}

configure<JavaApplication> {
  mainClass.set("zygarde.codegen.dsl.ModelMappingDslMainKt")
  applicationDefaultJvmArgs = listOf(
    "-Dzygarde.codegen.dsl.model-mapping.write-to=${
      project(":my-next-generated-model-mapping").file("src/main/kotlin").absolutePath
    }",
    "-Dzygarde.codegen.dsl.dto.write-to=${
      project(":my-next-generated-dto").file("src/main/kotlin").absolutePath
    }",
    "-Dzygarde.codegen.dsl.model-mapping.dto-package=com.example.dto",
    "-Dzygarde.codegen.dsl.model-mapping.extension-package=com.example.dto.extensions"
  )
}
```

Run the codegen with:

```bash
./gradlew :my-model-codegen:run
```

## Service Implementation

### Using Generated DAOs

Generated DAOs are Spring-managed beans. Inject them directly:

```kotlin
@Service
@Transactional
class EmployeeServiceImpl(
  private val employeeDao: EmployeeDao,
  private val departmentDao: DepartmentDao,
) : EmployeeService {
  // ...
}
```

### Entity-to-DTO with DtoBuilder

The generated `DtoBuilder` objects convert entities to DTOs:

```kotlin
override fun getEmployee(id: Int): EmployeeDto {
  return employeeDao.getReferenceById(id)
    .let { EmployeeDtoBuilder.build(it) }
}
```

For DTOs with extra fields, pass them as arguments:

```kotlin
override fun getEmployeeDetail(id: Int): EmployeeDetailDto {
  val employee = employeeDao.getReferenceById(id)
  val department = employee.department

  return EmployeeDetailDtoBuilder.build(
    employee,
    DepartmentDtoBuilder.build(department),   // fromRef
    employee.hireDate,                         // fromExtra: hireDate
    calculatePerformanceScore(employee),       // fromExtra: performanceScore
  )
}
```

### DTO-to-Entity with applyFrom

Generated `applyFrom` extensions apply request DTO fields to an entity:

```kotlin
override fun createEmployee(req: CreateEmployeeReq): EmployeeDetailDto {
  val department = departmentDao.getReferenceById(req.departmentId)

  return Employee(department = department)
    .applyFromCreateEmployeeReq(req)
    .let(employeeDao::saveAndFlush)
    .let { getEmployeeDetail(it.getId()) }
}

override fun updateEmployee(id: Int, req: UpdateEmployeeReq): EmployeeDetailDto {
  return employeeDao.getReferenceById(id)
    .applyFromUpdateEmployeeReq(req)
    .let(employeeDao::saveAndFlush)
    .let { getEmployeeDetail(it.getId()) }
}
```

!!! note
    Use `saveAndFlush` instead of `save` when you need the ID immediately after creation, or when subsequent operations depend on the persisted state.

### Search with Pagination

The `searchPage` + `toPageDto` pattern provides clean pagination:

```kotlin
override fun searchEmployees(req: SearchEmployeeReq): PageDto<EmployeeDto> {
  return employeeDao.searchPage(req) {
    req.name?.let { name() contains it }
    req.status?.let { status() eq it }
  }.toPageDto { EmployeeDtoBuilder.build(it) }
}
```

The `searchPage` method accepts a `PagingAndSortingRequest` and a search DSL lambda. The `toPageDto` method maps each entity in the result page to a DTO.

### Search with Relationship Filtering

Navigate relationships in search queries naturally:

```kotlin
override fun searchEmployeesByDepartment(
  departmentId: Int,
  req: SearchEmployeeReq
): PageDto<EmployeeDto> {
  return employeeDao.searchPage(req) {
    department().id() eq departmentId
    req.name?.let { name() contains it }
    req.status?.let { status() eq it }
  }.toPageDto { EmployeeDtoBuilder.build(it) }
}
```

### Count Queries

Use `searchCount` for counting without fetching entities:

```kotlin
val activeCount = employeeDao.searchCount {
  status() eq EmployeeStatus.ACTIVE
  department().id() eq departmentId
}
```

### Delete Operations

```kotlin
override fun deleteEmployee(id: Int) {
  employeeDao.deleteById(id)
}
```

## Module Dependency Flow

Understanding the module dependency graph is critical for build correctness:

```
Entity Module (my-core)
  Contains: @Entity classes, enums, shared types
  Dependencies: zygarde-jpa, zygarde-model-mapping, Spring Data JPA

        ↓ source shared via srcDirs

Core Codegen (my-core-codegen)          [KAPT]
  Runs: zygarde-jpa-codegen, zygarde-model-mapping-codegen
  Writes to: my-generated-jpa, my-generated-dto, my-generated-model-mapping

API Spec Codegen (my-api-spec-codegen)  [KAPT]
  Runs: zygarde-webmvc-codegen
  Writes to: my-generated-api-interface, my-generated-api-controller,
             my-generated-service-interface

Model Codegen (my-model-codegen)        [DSL]
  Runs: ModelMappingDslMain
  Writes to: my-next-generated-dto, my-next-generated-model-mapping

        ↓ generated code modules

Application (my-api)
  Depends on: all generated modules + zygarde runtime modules
  Contains: Service implementations, custom business logic, configuration
```

**Strict separation rules:**

1. **Codegen modules never depend on each other** - each reads from entity modules and writes to generated modules independently.
2. **Generated modules contain only generated code** - never add hand-written code to these modules.
3. **The application module never runs KAPT** - it only consumes generated code.
4. **Runtime modules never depend on codegen modules** - the framework's runtime is lightweight and independent.

## Two-Phase Generation

Zygarde supports using both KAPT and DSL codegen in the same project. This is useful when:

- **KAPT** handles entity-level generation (DAOs, search DSL, annotation-driven DTOs)
- **DSL** handles additional DTO mappings that are more complex or require cross-entity references

The two approaches complement each other:

| Aspect | KAPT | DSL |
|--------|------|-----|
| Trigger | Compile-time annotation processing | Runtime script execution |
| Input | `@ZyModel`, `@ApiProp`, `@ZyApi` annotations | Kotlin DSL spec classes |
| Best for | DAOs, search extensions, simple DTOs | Complex DTOs, cross-entity mappings |
| Regeneration | Requires recompilation | Run script independently |

### Workflow

1. Define entities with `@ZyModel` and `@ApiProp` annotations
2. Run KAPT: `./gradlew :my-core-codegen:kaptKotlin`
3. Define DSL specs referencing KAPT-generated DTOs
4. Run DSL: `./gradlew :my-model-codegen:run`
5. Build the full project: `./gradlew build`

## Testing

### Integration Tests with Feign Clients

Test generated APIs end-to-end using Feign clients:

```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
class EmployeeApiTest {

  @Test
  fun `should create and retrieve employee`() {
    val api = api<EmployeeApi>()

    // Create
    val created = api.createEmployee(
      CreateEmployeeReq(name = "John Doe", email = "john@example.com", departmentId = 1)
    )
    created.name shouldBe "John Doe"

    // Retrieve
    val retrieved = api.getEmployee(created.id)
    retrieved.email shouldBe "john@example.com"

    // Update
    api.updateEmployee(created.id, UpdateEmployeeReq(name = "Jane Doe"))
    api.getEmployee(created.id).name shouldBe "Jane Doe"

    // Delete
    api.deleteEmployee(created.id)
  }
}
```

### Unit Tests with MockK

Test service implementations in isolation:

```kotlin
class EmployeeServiceTest {

  private val employeeDao: EmployeeDao = mockk()
  private val departmentDao: DepartmentDao = mockk()
  private val service = EmployeeServiceImpl(employeeDao, departmentDao)

  @Test
  fun `should search employees by department`() {
    // given
    val employee = Employee(name = "John", department = mockk()).apply { id = 1 }
    every { employeeDao.searchPage(any(), any<EnhancedSearch<Employee>.() -> Unit>()) } returns
      PageImpl(listOf(employee))

    // when
    val result = service.searchEmployees(SearchEmployeeReq())

    // then
    result.items.size shouldBe 1
  }
}
```

## Common Pitfalls

### Never Edit Generated Code

Generated files in `codegen-generated/` modules are overwritten on each codegen run. Always modify the DSL spec or entity annotations instead.

### Keep Codegen Modules Out of CI

Codegen modules only need to run during development. Generated code should be committed to version control so CI can build without KAPT.

### Use saveAndFlush for Immediate Persistence

When returning a DTO from a create operation, use `saveAndFlush` to ensure the entity is persisted and its ID is available:

```kotlin
// Correct
Employee().applyFromCreateEmployeeReq(req).let(employeeDao::saveAndFlush)

// May cause issues if ID is accessed before flush
Employee().applyFromCreateEmployeeReq(req).let(employeeDao::save)
```

### Configure allOpen for All Entity Annotations

Missing `allOpen` configuration causes Hibernate proxy issues. Always include all three:

```kotlin
allOpen {
  annotation("javax.persistence.Entity")
  annotation("javax.persistence.MappedSuperclass")
  annotation("javax.persistence.Embeddable")
}
```

### Use Lazy Fetching for Relationships

Always use `FetchType.LAZY` for `@ManyToOne` and `@OneToMany` to avoid loading the entire entity graph:

```kotlin
@ManyToOne(targetEntity = Department::class, fetch = FetchType.LAZY)
open var department: Department
```

## Next Steps

- **[Code Generation](code-generation.md)** - Detailed code generation guide
- **[Search DSL](search-dsl.md)** - Type-safe query patterns
- **[Model Mapping](model-mapping.md)** - DTO mapping reference
- **[Common Patterns](../reference/common-patterns.md)** - Additional patterns
