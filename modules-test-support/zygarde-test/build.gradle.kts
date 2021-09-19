apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")
apply(plugin = "org.jetbrains.kotlin.plugin.spring")

dependencies {
  implementation(project(":zygarde-webmvc"))
  api("io.jsonwebtoken:jjwt-api:0.11.0")
  api("io.jsonwebtoken:jjwt-impl:0.11.0")
  api("io.jsonwebtoken:jjwt-jackson:0.11.0")
  api("io.kotest:kotest-assertions-shared-jvm:4.2.0")
  api("io.kotest:kotest-assertions-core-jvm:4.2.0")
  api("io.mockk:mockk:1.9.3")
  api("org.springframework.boot:spring-boot-starter-test") {
    exclude(group = "junit")
    exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
  }
  api("org.springframework.security:spring-security-test")
  api("org.springframework.cloud:spring-cloud-starter-openfeign")
}

tasks.getByName("bootJar").enabled = false
tasks.getByName("jar").enabled = true
tasks.getByName("printCoverage").enabled = false
