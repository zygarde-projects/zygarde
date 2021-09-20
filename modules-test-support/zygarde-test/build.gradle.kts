apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")
apply(plugin = "org.jetbrains.kotlin.plugin.spring")

dependencies {
  implementation(project(":zygarde-webmvc"))
  implementation("io.jsonwebtoken:jjwt-api:0.11.0")
  implementation("io.jsonwebtoken:jjwt-impl:0.11.0")
  implementation("io.jsonwebtoken:jjwt-jackson:0.11.0")
  implementation("io.kotest:kotest-assertions-shared-jvm:4.6.3")
  implementation("io.kotest:kotest-assertions-core-jvm:4.6.3")
  implementation("io.mockk:mockk:1.12.0")
  implementation("org.springframework.boot:spring-boot-starter-test")
  implementation("org.springframework.security:spring-security-test")
  implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
}

tasks.getByName("bootJar").enabled = false
tasks.getByName("jar").enabled = true
tasks.getByName("printCoverage").enabled = false
