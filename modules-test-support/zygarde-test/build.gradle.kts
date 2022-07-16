apply(plugin = "io.spring.dependency-management")
dependencies {
  api(project(":zygarde-test-error-handling"))
  api("io.kotest:kotest-assertions-shared-jvm:4.6.3")
  api("io.kotest:kotest-assertions-core-jvm:4.6.3")
  api("io.mockk:mockk:1.12.0")
}

tasks.getByName("printCoverage").enabled = false
