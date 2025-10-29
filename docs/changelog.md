# Changelog

All notable changes to Zygarde will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Comprehensive documentation site with MkDocs Material
- Automated documentation deployment via GitHub Actions

### Changed
- Improved documentation structure and organization

### Fixed
- Various documentation improvements and corrections

## [1.0.0] - 2024-01-01

### Added
- Initial release of Zygarde framework
- KAPT-based code generation for JPA entities
- Type-safe search DSL for JPA queries
- Enhanced DAO interfaces with search capabilities
- Entity base classes (AutoLongIdEntity, AutoIntIdEntity)
- Model mapping DSL for DTO generation
- WebMVC DSL for REST API generation
- Exception handling utilities
- Request tracing support
- Pagination and sorting helpers
- Envers integration for entity auditing
- JWT authentication utilities
- Jackson JSON serialization configuration
- Email sending utilities

### Features

#### JPA Extensions
- `@ZyModel` annotation for code generation
- Type-safe search DSL
- Enhanced DAO with search, remove, exists, and count operations
- Support for complex queries with AND/OR logic
- Automatic join resolution for relationships
- Search action types (String, Comparable, Boolean)

#### Code Generation
- KAPT annotation processor for DAOs and search DSL
- DSL-based model mapping generation
- DSL-based web API generation
- Configurable DAO naming and inheritance
- Aggregated Dao component generation

#### Web & REST
- Global exception handling with `ApiExceptionHandler`
- Custom exception mapper support
- Request tracing with `ApiTracingContext`
- Standardized error responses
- Bean validation integration
- CORS configuration helpers

#### Model Mapping
- Declarative DTO mapping definitions
- Entity-to-DTO and DTO-to-Entity mappings
- Reference and collection mapping
- Custom value providers
- Validation annotation support
- Field-level transformations

### Supported Technologies
- Kotlin 1.8.22
- Spring Boot 2.7.14
- Gradle 7.x+ (Kotlin DSL)
- JDK 8+
- JUnit Platform
- Kotest
- MockK

## Release Notes Template

Use this template for future releases:

```markdown
## [X.Y.Z] - YYYY-MM-DD

### Added
- New features

### Changed
- Changes to existing functionality

### Deprecated
- Soon-to-be removed features

### Removed
- Removed features

### Fixed
- Bug fixes

### Security
- Security fixes
```

## Version History

- **1.0.0** (2024-01-01) - Initial release

---

For upgrade instructions and migration guides, see the [documentation](https://zygarde-projects.github.io/zygarde/).

[Unreleased]: https://github.com/zygarde-projects/zygarde/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/zygarde-projects/zygarde/releases/tag/v1.0.0
