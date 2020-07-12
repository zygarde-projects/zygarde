apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")
apply(plugin = "org.jetbrains.kotlin.plugin.spring")

dependencies {
  implementation(project(":zygarde"))
  api("io.kotest:kotest-assertions:4.0.6")
  api("io.kotest:kotest-assertions-core-jvm:4.0.6")
  api("io.mockk:mockk:1.9.3")
  api("org.springframework.boot:spring-boot-starter-test")
  api("org.springframework.security:spring-security-test")
  api("org.springframework.cloud:spring-cloud-starter-openfeign")
}

tasks.getByName("bootJar").enabled = false
tasks.getByName("jar").enabled = true
tasks.getByName("printCoverage").enabled = false
