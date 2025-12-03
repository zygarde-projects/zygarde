apply(plugin = "org.springframework.boot")
apply(plugin = "org.jetbrains.kotlin.plugin.spring")

dependencies {
  api(project(":zygarde-error-handling"))
  api("io.kotest:kotest-assertions-shared-jvm:5.8.0")
  api("io.kotest:kotest-assertions-core-jvm:5.8.0")
  api("io.mockk:mockk:1.13.8")
}

tasks.getByName("bootJar").enabled = false
tasks.getByName("jar").enabled = true
