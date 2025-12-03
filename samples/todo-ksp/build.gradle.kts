plugins {
  id("org.springframework.boot")
  id("io.spring.dependency-management")
  id("org.jetbrains.kotlin.plugin.spring")
  id("com.google.devtools.ksp")
}

dependencies {
  ksp(project(":zygarde-jpa-codegen-ksp"))
  implementation(project(":zygarde-jpa"))
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-web")
  runtimeOnly("com.h2database:h2")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
}

ksp {
  arg("zygarde.codegen.base.package", "zygarde.samples.todo.generated")
  arg("zygarde.codegen.dao.package", "dao")
  arg("zygarde.codegen.entity.search", "search")
}

kotlin {
  sourceSets.main {
    kotlin.srcDir("build/generated/ksp/main/kotlin")
  }
}

// Exclude generated code from ktlint
configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
  filter {
    exclude { it.file.path.contains("/generated/") }
  }
}

tasks.getByName("bootJar").enabled = false
tasks.getByName("jar").enabled = true
