apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")
apply(plugin = "org.jetbrains.kotlin.plugin.spring")

dependencies {
  implementation(project(":zygarde-jpa"))
  implementation(project(":zygarde-core"))
  implementation(project(":zygarde-di"))
  implementation(project(":todo-src-core"))
  implementation(project(":todo-dsl-generated-api-interface"))
  implementation(project(":todo-dsl-generated-controller"))
  implementation(project(":todo-dsl-generated-model-mapping"))
  implementation(project(":todo-dsl-generated-service-interface"))
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")

  testImplementation(project(":zygarde-test"))
  testImplementation(project(":zygarde-test-feign"))
  testImplementation(project(":todo-dsl-generated-feign"))
  testImplementation("io.github.classgraph:classgraph:4.8.21")
  testImplementation("com.h2database:h2")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.getByName("bootJar").enabled = true
tasks.getByName("jar").enabled = false
