apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")

dependencies {
  implementation(project(":todo-src-core"))
  implementation(project(":zygarde-jpa"))
  implementation(project(":zygarde-codegen-dsl"))
}
tasks.getByName("bootJar").enabled = false
tasks.getByName("jar").enabled = true
tasks.getByName("printCoverage").enabled = false
tasks.getByName("ktlintMainSourceSetCheck").enabled = false
