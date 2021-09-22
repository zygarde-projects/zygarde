apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")
apply(plugin = "org.jetbrains.kotlin.plugin.spring")

dependencies {
  api(project(":zygarde-test-error-handling"))
  api("io.kotest:kotest-assertions-shared-jvm:4.6.3")
  api("io.kotest:kotest-assertions-core-jvm:4.6.3")
  api("io.mockk:mockk:1.12.0")
  implementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.getByName("bootJar").enabled = false
tasks.getByName("jar").enabled = true
tasks.getByName("printCoverage").enabled = false
