apply(plugin = "application")
apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")

dependencies {
  api(project(":todo-src-core"))
  implementation(project(":zygarde-core"))
}

tasks.getByName("bootJar").enabled = false
tasks.getByName("jar").enabled = true
tasks.getByName("bootDistTar").enabled = false
tasks.getByName("bootDistZip").enabled = false
tasks.getByName("bootStartScripts").enabled = false
tasks.getByName("ktlintMainSourceSetCheck").enabled = false
tasks.getByName("ktlintMainSourceSetFormat").enabled = false
