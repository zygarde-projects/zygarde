apply(plugin = "io.spring.dependency-management")
dependencies {
  api(project(":zygarde-test-error-handling"))
  api("io.kotest:kotest-assertions-shared-jvm:5.9.0")
  api("io.kotest:kotest-assertions-core-jvm:5.9.0")
  api("io.mockk:mockk:1.13.10")
}

// tasks.getByName("printCoverage").enabled = false
