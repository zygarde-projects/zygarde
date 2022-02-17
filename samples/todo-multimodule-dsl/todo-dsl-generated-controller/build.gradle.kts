apply(plugin = "application")
apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")

dependencies {
  implementation(project(":zygarde-core"))
  implementation(project(":zygarde-di"))
  implementation(project(":todo-dsl-generated-model-mapping"))
  implementation(project(":todo-dsl-generated-api-interface"))
  implementation(project(":todo-dsl-generated-service-interface"))
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-validation")
}
tasks.getByName("bootJar").enabled = false
tasks.getByName("jar").enabled = true
tasks.getByName("printCoverage").enabled = false
tasks.getByName("ktlintMainSourceSetCheck").enabled = false
