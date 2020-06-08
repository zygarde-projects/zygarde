apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")
apply(plugin = "org.jetbrains.kotlin.plugin.spring")

dependencies {
  implementation(project(":zygarde"))
  implementation("io.kotest:kotest-runner-junit5:4.0.6")
  implementation("io.kotest:kotest-assertions:4.0.6")
  implementation("io.kotest:kotest-assertions-core-jvm:4.0.6")
  api("org.springframework.cloud:spring-cloud-starter-openfeign")
}

tasks.getByName("bootJar").enabled = false
tasks.getByName("jar").enabled = true
tasks.getByName("printCoverage").enabled = false
