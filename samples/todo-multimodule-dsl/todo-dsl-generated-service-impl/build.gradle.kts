apply(plugin = "application")
apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")
apply(plugin = "org.jetbrains.kotlin.plugin.spring")

dependencies {
  implementation(project(":zygarde-jpa"))
  implementation(project(":todo-src-core"))
  implementation(project(":todo-dsl-generated-dto"))
  implementation(project(":todo-dsl-generated-model-mapping"))
  implementation(project(":todo-dsl-generated-service-interface"))
  implementation("org.springframework.boot:spring-boot-starter")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
}

tasks.getByName("bootJar").enabled = false
tasks.getByName("jar").enabled = true
tasks.getByName("bootDistTar").enabled = false
tasks.getByName("bootDistZip").enabled = false
tasks.getByName("bootStartScripts").enabled = false
tasks.getByName("ktlintMainSourceSetCheck").enabled = false
tasks.getByName("ktlintMainSourceSetFormat").enabled = false
