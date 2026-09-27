# Spring Boot 4 and Java 24 migration

The `v4` branch upgrades all Zygarde modules, processors, and sample applications
to one Spring Boot 4 dependency set. Java 24 is required for compiling Zygarde and
for running its published bytecode.

## Versions

| Component | Version |
| --- | --- |
| Spring Boot Gradle plugin and BOM | 4.0.8 |
| Spring Cloud BOM | 2025.1.3 |
| Gradle wrapper | 8.14.4 |
| Kotlin Gradle plugins | 2.2.20 |
| KSP | 2.2.20-2.0.4 |
| Java toolchain, source, target, and Kotlin JVM target | 24 |
| springdoc-openapi | 3.1.1 |
| ktlint Gradle plugin / ktlint | 14.2.0 / 1.8.0 |
| detekt Gradle plugin | 2.0.0-alpha.1 |

The Gradle wrapper can run with an installed Java 24 JDK. If SDKMAN manages your
JDKs, list its available Java 24 candidates with `sdk list java`, install the
chosen candidate with `sdk install java <candidate>`, then activate it with
`sdk use java <candidate>`. The repository also configures the Foojay Gradle
toolchain resolver, so Gradle can download a Java 24 toolchain when it is not
already installed. `./gradlew -version` shows the JDK used to run Gradle;
`java.toolchain` controls the JDK used to compile and test the modules.

## Migration notes

- Boot 4's modular Web MVC, JPA, GraphQL, and test starters replace their older
  starter names. Boot auto-configuration and `TestRestTemplate` imports moved to
  new packages. HTTP, GraphQL, Feign, and JPA sample tests cover these changes.
- `zygarde-jackson` and downstream JSON handling now use Jackson 3
  (`tools.jackson`). Callers that compile against its Jackson types need to
  migrate their imports. JSON patch and serialization tests cover the behavior.
- Spring Data JPA 4 requires non-null entity type bounds in repository helpers
  and allows a nullable Criteria query in `Specification` callbacks. Those
  signatures are updated in `zygarde-jpa`.
- KAPT and KSP processor tests use Kotlin 2.2 compatible compile testing
  libraries. The KSP JPA processor resolves inherited `@Id` properties and the
  generated enum helper uses unqualified enum value names.
- The DSL sample outputs were regenerated with their generators. Do not edit the
  tracked generated Kotlin sources directly.
- detekt 2 currently uses an alpha plugin. Per-module `detekt-baseline-main.xml`
  files record findings present at migration time; `detektMain` rejects new
  findings. `detekt` alone does not analyze the main source sets in this build.
- Published POM generation was checked locally for the Boot 4 and Cloud 2025.1
  dependency graph. Publication to Maven Central was not performed.

## Verification

Run from the repository root with a Java 24 JDK available, or let Gradle obtain
its Java 24 toolchain through Foojay:

```bash
./gradlew ktlintCheck --continue --console=plain
./gradlew detektMain --continue --console=plain
./gradlew build --continue --console=plain
./gradlew generatePomFileForDefaultPublication --continue --console=plain
```

All four commands passed locally on 2026-09-27 using Java 24. `build` includes
the unit and sample integration tests. The tracked DSL samples were regenerated
successfully with the DSL API, GraphQL API, and SQL API generator tasks.

## References

- [Spring Boot 4.0 migration guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)
- [Spring Boot 4.0 system requirements](https://docs.spring.io/spring-boot/4.0/system-requirements.html)
- [Spring Cloud release train compatibility](https://spring.io/projects/spring-cloud/)
