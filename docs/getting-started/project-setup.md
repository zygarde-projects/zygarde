# Project Setup

This guide covers setting up a complete multi-module project with Zygarde, including best practices for project structure and configuration.

## Multi-Module Project Structure

For larger applications, organize your project into multiple modules:

```
my-app/
├── settings.gradle.kts
├── build.gradle.kts
├── my-app-domain/          # Domain models
│   ├── build.gradle.kts
│   └── src/main/kotlin/
│       └── model/
├── my-app-codegen/         # Generated code
│   ├── build.gradle.kts
│   └── src/main/kotlin/
│       └── codegen/
├── my-app-service/         # Business logic
│   ├── build.gradle.kts
│   └── src/main/kotlin/
│       └── service/
└── my-app-web/             # REST controllers
    ├── build.gradle.kts
    └── src/main/kotlin/
        └── controller/
```

## Root Configuration

### settings.gradle.kts

```kotlin
rootProject.name = "my-app"

include(
  "my-app-domain",
  "my-app-codegen",
  "my-app-service",
  "my-app-web"
)
```

### Root build.gradle.kts

```kotlin
buildscript {
  repositories {
    mavenCentral()
  }
}

plugins {
  kotlin("jvm") version "1.8.22" apply false
  kotlin("kapt") version "1.8.22" apply false
  kotlin("plugin.spring") version "1.8.22" apply false
  id("org.springframework.boot") version "2.7.14" apply false
  id("io.spring.dependency-management") version "1.1.3" apply false
}

allprojects {
  group = "com.example.myapp"
  version = "1.0.0-SNAPSHOT"

  repositories {
    mavenCentral()
    maven("https://nexus.puni.tw/repository/maven-releases")
  }
}

subprojects {
  apply(plugin = "kotlin")

  dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
  }
}
```

## Module Configuration

### Domain Module (my-app-domain)

Contains entity definitions:

```kotlin
// my-app-domain/build.gradle.kts
plugins {
  kotlin("jvm")
  kotlin("plugin.spring")
  kotlin("kapt")
}

dependencies {
  api("org.springframework.boot:spring-boot-starter-data-jpa:2.7.14")
  api("zygarde:zygarde-jpa:VERSION")
  kapt("zygarde:zygarde-jpa-codegen:VERSION")
}

kapt {
  arguments {
    arg("zygarde.codegen.base.package", "com.example.myapp.codegen")
    arg("zygarde.codegen.dao.inherit", "zygarde.data.jpa.dao.ZygardeEnhancedDao")
  }
}
```

### Codegen Module (my-app-codegen)

Receives generated code from domain module:

```kotlin
// my-app-codegen/build.gradle.kts
plugins {
  kotlin("jvm")
  kotlin("plugin.spring")
}

dependencies {
  api(project(":my-app-domain"))
  api("org.springframework.boot:spring-boot-starter-data-jpa:2.7.14")
  api("zygarde:zygarde-jpa:VERSION")
}

sourceSets {
  main {
    java {
      srcDir("${project(":my-app-domain").buildDir}/generated/source/kapt/main")
    }
  }
}

tasks.compileKotlin {
  dependsOn(":my-app-domain:kaptKotlin")
}
```

### Service Module (my-app-service)

Contains business logic:

```kotlin
// my-app-service/build.gradle.kts
plugins {
  kotlin("jvm")
  kotlin("plugin.spring")
}

dependencies {
  api(project(":my-app-codegen"))
  implementation("org.springframework.boot:spring-boot-starter:2.7.14")
  implementation("zygarde:zygarde-core:VERSION")

  testImplementation("org.springframework.boot:spring-boot-starter-test:2.7.14")
  testImplementation("io.mockk:mockk:1.12.0")
}
```

### Web Module (my-app-web)

Spring Boot application and controllers:

```kotlin
// my-app-web/build.gradle.kts
plugins {
  kotlin("jvm")
  kotlin("plugin.spring")
  id("org.springframework.boot")
  id("io.spring.dependency-management")
}

dependencies {
  implementation(project(":my-app-service"))
  implementation("org.springframework.boot:spring-boot-starter-web:2.7.14")
  implementation("zygarde:zygarde-webmvc:VERSION")

  runtimeOnly("com.h2database:h2")

  testImplementation("org.springframework.boot:spring-boot-starter-test:2.7.14")
}

springBoot {
  mainClass.set("com.example.myapp.ApplicationKt")
}
```

## Database Configuration

### H2 In-Memory (Development)

```yaml
# src/main/resources/application-dev.yml
spring:
  datasource:
    url: jdbc:h2:mem:myapp
    driver-class-name: org.h2.Driver
    username: sa
    password:
  h2:
    console:
      enabled: true
      path: /h2-console
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
```

### PostgreSQL (Production)

```yaml
# src/main/resources/application-prod.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/myapp
    driver-class-name: org.postgresql.Driver
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

## Code Quality Tools

### ktlint Configuration

```kotlin
// Root build.gradle.kts
plugins {
  id("org.jlleitschuh.gradle.ktlint") version "10.2.0"
}

allprojects {
  apply(plugin = "org.jlleitschuh.gradle.ktlint")

  ktlint {
    version.set("0.45.2")
    verbose.set(true)
    android.set(false)
  }
}
```

### Detekt Configuration

```kotlin
plugins {
  id("io.gitlab.arturbosch.detekt") version "1.18.1"
}

detekt {
  buildUponDefaultConfig = true
  config = files("${rootProject.projectDir}/detekt.yml")
}
```

## Testing Setup

### Integration Tests

```kotlin
@SpringBootTest
@TestPropertySource(locations = ["classpath:application-test.yml"])
class TodoServiceIntegrationTest {

  @Autowired
  lateinit var todoService: TodoService

  @Test
  fun `should create and retrieve todo`() {
    // given
    val title = "Test Todo"

    // when
    val created = todoService.createTodo(title, null)
    val retrieved = todoService.getTodoById(created.id!!)

    // then
    retrieved shouldNotBe null
    retrieved?.title shouldBe title
  }
}
```

### Repository Tests

```kotlin
@DataJpaTest
class TodoDaoTest {

  @Autowired
  lateinit var todoDao: TodoDao

  @Test
  fun `should find todos by title`() {
    // given
    val todo1 = Todo(title = "Kotlin Tutorial", completed = false)
    val todo2 = Todo(title = "Java Tutorial", completed = false)
    todoDao.saveAll(listOf(todo1, todo2))

    // when
    val results = todoDao.search {
      title() contains "Kotlin"
    }

    // then
    results.size shouldBe 1
    results[0].title shouldBe "Kotlin Tutorial"
  }
}
```

## Build and Run

### Build All Modules

```bash
./gradlew build
```

### Run Specific Module

```bash
./gradlew :my-app-web:bootRun
```

### Run Tests

```bash
# All tests
./gradlew test

# Specific module
./gradlew :my-app-service:test

# With coverage
./gradlew test jacocoTestReport
```

### Clean Build

```bash
./gradlew clean build
```

## Zygarde Project Structure Tips

### 1. Separate Generated Code Module

Keep Zygarde-generated code in a dedicated module:

```
my-app/
├── my-app-domain/       # Entities with @ZyModel
├── my-app-codegen/      # Zygarde-generated code
└── my-app-service/      # Uses generated Dao
```

This avoids mixing hand-written and generated code.

### 2. Configure KAPT Base Package

Set consistent package for generated code:

```kotlin
kapt {
  arguments {
    arg("zygarde.codegen.base.package", "com.example.codegen")
    arg("zygarde.codegen.dao.package.postfix", "dao")
  }
}
```

## Next Steps

- **[Architecture →](../guide/architecture.md)** - Understand module dependencies
- **[JPA Extensions →](../guide/jpa-extensions.md)** - Learn advanced features
- **[DSL Tutorial →](../tutorials/dsl-based.md)** - Explore DSL-based generation
