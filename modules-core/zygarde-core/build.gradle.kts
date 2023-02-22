apply(plugin = "org.springframework.boot")
apply(plugin = "org.jetbrains.kotlin.plugin.spring")

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-logging")
  api(project(":zygarde-error-handling"))
  api("io.swagger.core.v3:swagger-annotations:2.1.10")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation(project(":zygarde-test-error-handling"))
  kapt("org.springframework.boot:spring-boot-configuration-processor")
}

tasks.getByName("bootJar").enabled = false
tasks.getByName("jar").enabled = true
