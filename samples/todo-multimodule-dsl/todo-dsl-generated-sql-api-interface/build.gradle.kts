apply(plugin = "application")
apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")

dependencies {
  implementation(project(":zygarde-core"))
  implementation(project(":todo-dsl-generated-sql-api-dto"))
}

tasks.getByName("bootJar").enabled = false
tasks.getByName("jar").enabled = true
tasks.getByName("bootDistTar").enabled = false
tasks.getByName("bootDistZip").enabled = false
tasks.getByName("bootStartScripts").enabled = false
tasks.getByName("ktlintMainSourceSetCheck").enabled = false
tasks.getByName("ktlintMainSourceSetFormat").enabled = false
