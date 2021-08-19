apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")

dependencies {
  api(project(":zygarde-core"))
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  api("io.swagger:swagger-annotations:1.6.0")
  testApi("org.springframework.boot:spring-boot-starter-test") {
    exclude(group = "junit")
    exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
  }
}

tasks.getByName("bootJar").enabled = false
tasks.getByName("printCoverage").enabled = false
tasks.getByName("jar").enabled = true
