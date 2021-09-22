apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")
apply(plugin = "org.jetbrains.kotlin.plugin.spring")

dependencies {
  api(project(":zygarde-test"))
  api(project(":zygarde-web"))
  api(project(":zygarde-jackson"))
  api("io.jsonwebtoken:jjwt-api:0.11.0")
  api("io.jsonwebtoken:jjwt-impl:0.11.0")
  api("io.jsonwebtoken:jjwt-jackson:0.11.0")
  api("org.springframework.boot:spring-boot-starter-test")
  api("org.springframework.security:spring-security-test")
  api("org.springframework.cloud:spring-cloud-starter-openfeign")
}

tasks.getByName("bootJar").enabled = false
tasks.getByName("jar").enabled = true
tasks.getByName("printCoverage").enabled = false
