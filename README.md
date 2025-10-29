# Zygarde

**Zygarde** is a powerful Kotlin framework for simplifying enterprise application development with Spring Boot. It provides code generation, JPA enhancements with type-safe search DSL, model mapping, and web/REST utilities.

[![Documentation](https://img.shields.io/badge/docs-latest-blue.svg)](https://zygarde-projects.github.io/zygarde/)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.8.22-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.14-green.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

## Features

### =� Two-Tier Code Generation

- **KAPT-Based**: Compile-time annotation processing for JPA entities
- **DSL-Based**: Runtime DSL scripts for application layer code generation

### =
 Type-Safe Search DSL

Write expressive, type-safe queries without JPA Criteria complexity:

```kotlin
bookDao.search {
  title() contains "Kotlin"
  author().country() eq "USA"
  publishedYear() gte 2020
}
```

### = Declarative Model Mapping

Define DTO mappings with automatic code generation:

```kotlin
BookDto {
  fromAutoLongId(Book::id)
  from(Book::title)
  from(Book::description)
}
```

### < Web & REST Utilities

Complete REST API layer generation with consistent error handling and validation.

## Quick Start

### Add Dependencies

```kotlin
// build.gradle.kts
plugins {
  kotlin("jvm") version "1.8.22"
  kotlin("kapt") version "1.8.22"
}

dependencies {
  implementation("zygarde:zygarde-jpa:VERSION")
  kapt("zygarde:zygarde-jpa-codegen:VERSION")
}

repositories {
  maven("https://nexus.puni.tw/repository/maven-releases")
}
```

### Annotate Your Entities

```kotlin
@Entity
@ZyModel
data class Book(
  @Id @GeneratedValue
  val id: Long? = null,
  val title: String,
  val author: Author
) : AutoLongIdEntity()
```

### Build and Use Generated Code

```bash
./gradlew kaptKotlin
```

```kotlin
@Service
class BookService(private val dao: Dao) {

  fun searchBooks(keyword: String) = dao.book.search {
    title() containsIgnoreCase keyword
  }
}
```

## Documentation

**=� [Full Documentation](https://zygarde-projects.github.io/zygarde/)**

- [Getting Started](https://zygarde-projects.github.io/zygarde/getting-started/) - Installation and setup
- [User Guide](https://zygarde-projects.github.io/zygarde/guide/architecture/) - Architecture and features
- [Tutorials](https://zygarde-projects.github.io/zygarde/tutorials/kapt-based/) - Step-by-step guides
- [API Reference](https://zygarde-projects.github.io/zygarde/api/) - Complete API documentation
- [Reference](https://zygarde-projects.github.io/zygarde/reference/kapt-options/) - Configuration options

## Examples

Check out the sample applications:

- [todo-legacy](samples/todo-legacy/) - KAPT-based TODO app
- [todo-multimodule-dsl](samples/todo-multimodule-dsl/) - DSL-based multi-module TODO app

## Modules

- **zygarde-core** - Core utilities and common functionality
- **zygarde-jpa** - JPA extensions and search DSL
- **zygarde-jpa-codegen** - KAPT annotation processor
- **zygarde-webmvc** - Spring WebMVC utilities
- **zygarde-model-mapping-core** - Model mapping abstractions
- **zygarde-model-mapping-codegen-dsl** - DSL for DTO generation

[See all modules �](https://zygarde-projects.github.io/zygarde/reference/modules/)

## Tech Stack

- **Kotlin** 1.8.22
- **Spring Boot** 2.7.14
- **Gradle** 7.x+ (Kotlin DSL)
- **JDK** 8+

## Build Commands

```bash
# Full build with tests
./gradlew build

# Format code
./gradlew ktlintFormat

# Run tests
./gradlew test

# Run sample application
./gradlew :samples:todo-legacy:bootRun
```

## Contributing

We welcome contributions! Please see our [Contributing Guide](https://zygarde-projects.github.io/zygarde/development/contributing/) for details.

### Quick Contribution Steps

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'feat: add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## Development

### Pre-commit Checklist

- [ ] Run `./gradlew ktlintFormat`
- [ ] Run `./gradlew build` (all tests pass)
- [ ] Run `./gradlew detekt` (no violations)
- [ ] Update documentation if needed

See [Development Guide](https://zygarde-projects.github.io/zygarde/development/coding-standards/) for detailed guidelines.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Links

- [Documentation](https://zygarde-projects.github.io/zygarde/)
- [API Reference](https://zygarde-projects.github.io/zygarde/api/)
- [Changelog](https://zygarde-projects.github.io/zygarde/changelog/)
- [Issue Tracker](https://github.com/zygarde-projects/zygarde/issues)
- [Discussions](https://github.com/zygarde-projects/zygarde/discussions)

## Repositories

- **Nexus**: `https://nexus.puni.tw/repository/maven-releases`
- **GitHub Packages**: `https://maven.pkg.github.com/zygarde-projects/zygarde`

## Support

- **Documentation**: https://zygarde-projects.github.io/zygarde/
- **Issues**: https://github.com/zygarde-projects/zygarde/issues
- **Discussions**: https://github.com/zygarde-projects/zygarde/discussions

---

Made with d by the Zygarde team
