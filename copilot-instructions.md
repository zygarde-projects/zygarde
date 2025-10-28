# Copilot Instructions for Zygarde

This document provides comprehensive guidance for GitHub Copilot and AI-assisted development in the Zygarde project.

## Project Overview

Zygarde is a multi-module Kotlin framework designed to simplify enterprise application development. It provides code generation support, JPA enhancements, web utilities, and model mapping capabilities for building robust Kotlin/Spring applications.

### Technology Stack
- **Language**: Kotlin 1.8.22
- **Build Tool**: Gradle 7.x+ with Kotlin DSL
- **Framework**: Spring Boot 2.7.14
- **Testing**: JUnit Platform, Kotest assertions, MockK
- **Quality Tools**: ktlint, detekt, JaCoCo
- **Code Generation**: KotlinPoet, KAPT

## Architecture & Module Organization

### Module Structure
```
zygarde/
├── modules-core/           # Core utilities and base functionality
├── modules-jpa/            # JPA extensions and helpers
├── modules-web/            # Web/REST utilities (WebMVC, WebFlux)
├── modules-model-mapping/  # Object mapping support
├── modules-codegen-support/# Code generation infrastructure
├── modules-test-support/   # Shared test fixtures and utilities
├── modules-bom/            # Bill of Materials for dependency management
├── samples/                # Example applications
└── doc/                    # Design documentation and guides
```

### Package Naming
- All production packages should be under `zygarde.*` namespace
- Sample applications use their own namespaces (e.g., `zygarde.samples.todo`)
- Generated code goes in dedicated `*-codegen` modules or `doc/codegen-*`

### Source Layout
Every module follows standard Kotlin conventions:
```
module-name/
├── src/main/kotlin/        # Production code
├── src/test/kotlin/        # Unit tests
└── build.gradle.kts        # Module build configuration
```

## Development Workflow

### Essential Commands

#### Building & Testing
```bash
# Full build with tests and coverage
./gradlew build

# Run tests only
./gradlew test

# Test specific module
./gradlew -p modules-core/zygarde-core test

# Run sample application
./gradlew :samples:todo-legacy:bootRun
```

#### Code Quality
```bash
# Check code formatting
./gradlew ktlintCheck

# Auto-format code
./gradlew ktlintFormat

# Run static analysis
./gradlew detekt

# Coverage is automatically reported after test runs
# via jacocoTestReport and printCoverage tasks
```

#### Dependency Management
```bash
# Refresh dependency locks
./scripts/deps.sh
```

### Pre-Commit Checklist
Before committing or opening a PR:
1. Run `./gradlew ktlintFormat` to fix formatting
2. Run `./gradlew build` to ensure all tests pass
3. Run `./gradlew detekt` to verify static analysis
4. Check that coverage has not decreased
5. If touching code generation, regenerate samples and verify diffs

## Coding Standards

### Kotlin Style Guide

#### Formatting
- **Indentation**: 2 spaces (configured in `.editorconfig`)
- **Line Length**: 150 characters soft limit
- **Spacing**: Spaces over tabs
- **Imports**: Explicit imports only; no wildcards

#### Naming Conventions
- **Classes**: PascalCase (e.g., `UserRepository`, `EntityMapper`)
- **Functions/Properties**: camelCase (e.g., `findById`, `userName`)
- **Constants**: UPPER_SNAKE_CASE for compile-time constants
- **Test Classes**: `<ClassName>Test` (e.g., `UserRepositoryTest`)

#### Code Organization
- One class per file (unless inner/sealed classes)
- Order: properties → init blocks → functions → companion objects
- Group related functions together
- Prefer `private` visibility unless broader scope is needed

### Testing Standards

#### Test Structure
```kotlin
class UserServiceTest {
  @Test
  fun `should create user with valid data`() {
    // given
    val userData = UserData(name = "John", email = "john@example.com")
    
    // when
    val result = userService.createUser(userData)
    
    // then
    result shouldNotBe null
    result.name shouldBe "John"
  }
}
```

#### Testing Guidelines
- Use Kotest assertions (`shouldBe`, `shouldNotBe`, `shouldThrow`, etc.)
- Use MockK for mocking (`mockk<T>()`, `every`, `verify`)
- Adopt `should`/`given`/`when`/`then` style for readability
- Test file location mirrors production code package structure
- Aim to maintain or increase test coverage
- Name tests descriptively using backticks for readability

#### Test Fixtures
- Reusable test utilities go in `modules-test-support`
- Keep test data builders and fixtures consistent across modules

## Code Generation Guidelines

### When Working with Generators
- Code generation logic lives in `modules-codegen-support` and `*-codegen` modules
- After modifying DSLs or generators:
  1. Regenerate sample outputs in `samples/todo-multimodule-dsl`
  2. Review generated code diffs in version control
  3. Ensure generated code compiles and passes tests
  
### Generated Code Isolation
- Keep generated code in dedicated modules or directories
- Never manually edit generated files
- Document generation commands in module README

## Git & Version Control

### Commit Message Format
Follow Conventional Commits:
```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types**: `feat`, `fix`, `refactor`, `chore`, `docs`, `test`, `perf`, `style`, `ci`

**Examples**:
```
feat(jpa): add support for composite key entities
fix(web): correct JSON serialization for LocalDateTime
chore(deps): upgrade Spring Boot to 2.7.14
docs(codegen): document DSL usage patterns
```

### Pull Request Guidelines
When opening a PR:
1. **Title**: Use conventional commit format
2. **Description**:
   - Concise summary of behavior changes
   - Reference related issues (e.g., "Fixes #123")
   - Document new configuration or migration steps
   - Include screenshots/logs for UI or HTTP-facing changes
3. **Verification**:
   - All builds, ktlint, detekt, and tests pass locally
   - Coverage maintained or improved
   - Documentation updated if needed

### Branch Strategy
- Keep branches focused on single features/fixes
- Use descriptive branch names (e.g., `feat/composite-keys`, `fix/datetime-serialization`)
- Rebase on main before opening PR to resolve conflicts early

## Common Development Tasks

### Adding a New Module
1. Create directory under appropriate `modules-*` category
2. Add `build.gradle.kts` with module configuration
3. Create standard `src/main/kotlin` and `src/test/kotlin` structure
4. Module is auto-discovered by `registerModules()` in `settings.gradle.kts`

### Adding a New Feature
1. Create feature branch from main
2. Implement feature with tests (TDD encouraged)
3. Run `./gradlew ktlintFormat && ./gradlew build`
4. Document public APIs with KDoc
5. Update relevant module README if needed
6. Open PR with clear description

### Fixing a Bug
1. Write failing test that reproduces the bug
2. Implement fix to make test pass
3. Verify no regressions with full test suite
4. Document fix in commit message

### Refactoring Code
1. Ensure full test coverage of code to refactor
2. Make incremental changes
3. Run tests after each step
4. Use IDE refactoring tools when possible
5. Commit logical refactoring steps separately

## Publishing & Release

### Publishing Artifacts
Artifacts are published to:
- Private Nexus repository (requires `PUNI_NEXUS_*` env vars)
- GitHub Packages (requires `ZYGARDE_GH_*` env vars)

Scripts:
- `./publish.sh` - Publish to Nexus
- `./publish-gh.sh` - Publish to GitHub Packages

### Environment Variables
```bash
# Nexus publishing
export PUNI_NEXUS_DEPLOY_USER=<username>
export PUNI_NEXUS_DEPLOY_PASSWORD=<password>

# GitHub publishing
export ZYGARDE_GH_USERNAME=<github-username>
export ZYGARDE_GH_TOKEN=<github-token>
```

## AI Assistant Guidelines

### When to Suggest Changes
- **DO** suggest improvements that align with existing patterns
- **DO** recommend better Kotlin idioms and standard library usage
- **DO** help maintain consistency across modules
- **DO** suggest adding missing tests or improving coverage
- **DON'T** suggest changes to generated code
- **DON'T** recommend wildcard imports
- **DON'T** propose breaking changes without discussion

### Code Generation Suggestions
- Follow existing patterns in `modules-codegen-support`
- Use KotlinPoet for type-safe code generation
- Generate readable, well-formatted code
- Include proper package declarations and imports

### Test Generation
- Mirror production code structure in test packages
- Use Kotest assertions consistently
- Include happy path and edge cases
- Mock external dependencies with MockK

### Documentation
- Add KDoc for public APIs
- Use inline comments sparingly, only for complex logic
- Keep comments up-to-date with code changes
- Prefer self-documenting code over excessive comments

## Troubleshooting

### Common Issues

**Build fails with KAPT errors**
- Clean build: `./gradlew clean build`
- Check KAPT configuration in `build.gradle.kts`
- Verify annotation processors are on classpath

**ktlint failures**
- Auto-fix: `./gradlew ktlintFormat`
- Review `.editorconfig` for project standards

**Test failures**
- Run specific test: `./gradlew test --tests "ClassName.testMethod"`
- Check test isolation (ensure tests don't depend on execution order)
- Verify MockK setup/cleanup in tests

**Coverage decreased**
- Identify uncovered lines with JaCoCo HTML report in `build/reports/jacoco`
- Add missing test cases
- Consider if new code requires integration tests

## Resources

### Documentation
- Project documentation: `doc/` directory
- Code generation guides: `doc/codegen.md`
- Module-specific docs: `modules-*/README.md` (where applicable)

### External References
- [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- [Kotest Documentation](https://kotest.io/)
- [MockK Documentation](https://mockk.io/)
- [Conventional Commits](https://www.conventionalcommits.org/)
- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/2.7.14/reference/)

## Quick Reference

### File Locations
| What | Where |
|------|-------|
| Core utilities | `modules-core/zygarde-core/` |
| JPA extensions | `modules-jpa/zygarde-jpa/` |
| Web utilities | `modules-web/zygarde-webmvc/`, `zygarde-webflux/` |
| Code generation | `modules-codegen-support/` |
| Test fixtures | `modules-test-support/` |
| Sample apps | `samples/` |
| Documentation | `doc/` |
| Build scripts | Root `build.gradle.kts`, `settings.gradle.kts` |

### Key Gradle Tasks
| Task | Purpose |
|------|---------|
| `./gradlew build` | Full build + test + coverage |
| `./gradlew test` | Run all tests |
| `./gradlew ktlintCheck` | Check formatting |
| `./gradlew ktlintFormat` | Auto-format code |
| `./gradlew detekt` | Static analysis |
| `./gradlew clean` | Clean build artifacts |

---

**Last Updated**: 2025-10-28  
**Maintainer**: See repository contributors
