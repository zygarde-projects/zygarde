apply(plugin = "io.spring.dependency-management")
dependencies {
  api(project(":zygarde-test-error-handling"))
  api("io.kotest:kotest-assertions-shared-jvm:5.8.0")
  api("io.kotest:kotest-assertions-core-jvm:5.8.0")
  api("io.mockk:mockk:1.13.8")
}
