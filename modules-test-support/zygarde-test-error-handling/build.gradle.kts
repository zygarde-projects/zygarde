apply(plugin = "org.springframework.boot")
apply(plugin = "org.jetbrains.kotlin.plugin.spring")

dependencies {
  api(project(":zygarde-error-handling"))
  api("io.kotest:kotest-assertions-shared-jvm:4.6.3")
  api("io.kotest:kotest-assertions-core-jvm:4.6.3")
  api("io.mockk:mockk:1.12.0")
}

tasks.getByName("bootJar").enabled = false
tasks.getByName("jar").enabled = true
tasks.getByName("printCoverage").enabled = false
