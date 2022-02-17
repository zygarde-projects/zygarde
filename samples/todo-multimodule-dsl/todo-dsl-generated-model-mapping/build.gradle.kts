apply(plugin = "application")
apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")

dependencies {
  implementation(project(":todo-src-core"))
  implementation(project(":zygarde-model-mapping"))
}
tasks.getByName("bootJar").enabled = false
tasks.getByName("jar").enabled = true
tasks.getByName("printCoverage").enabled = false
tasks.getByName("ktlintMainSourceSetCheck").enabled = false
