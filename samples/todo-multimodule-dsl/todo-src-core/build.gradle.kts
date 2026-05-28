apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")
apply(plugin = "org.jetbrains.kotlin.plugin.spring")

dependencies {
  kapt(project(":zygarde-model-mapping-codegen"))
  kapt(project(":zygarde-jpa-codegen"))
  implementation(project(":zygarde-jpa"))
  implementation(project(":zygarde-sql-api"))
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("jakarta.validation:jakarta.validation-api")
}

kapt {
  arguments {
    arg("zygarde.codegen.base.package", "example.codegen")
  }
}

tasks.getByName("bootJar").enabled = false
tasks.getByName("jar").enabled = true
