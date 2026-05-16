apply(plugin = "application")
apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")

dependencies {
  implementation(project(":zygarde-core"))
  implementation(project(":zygarde-di"))
  implementation(project(":todo-src-core"))
  implementation(project(":todo-dsl-generated-dto"))
  implementation(project(":todo-dsl-generated-graphql-service-interface"))
  implementation("org.springframework.boot:spring-boot-starter-graphql")
}
tasks.getByName("bootJar").enabled = false
tasks.getByName("jar").enabled = true
tasks.getByName("bootDistTar").enabled = false
tasks.getByName("bootDistZip").enabled = false
tasks.getByName("bootStartScripts").enabled = false
tasks.getByName("ktlintMainSourceSetCheck").enabled = false
tasks.getByName("ktlintMainSourceSetFormat").enabled = false
