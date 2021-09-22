apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")
apply(plugin = "org.jetbrains.kotlin.plugin.spring")

dependencies {
  api(project(":zygarde-jpa"))
  api(project(":zygarde-core"))
  api(project(":zygarde-jackson"))
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation(project(":zygarde-test"))
}

tasks.getByName("printCoverage").enabled = false
tasks.getByName("bootJar").enabled = false
tasks.getByName("jar").enabled = true
