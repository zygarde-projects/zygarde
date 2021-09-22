apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")
apply(plugin = "org.jetbrains.kotlin.plugin.spring")

dependencies {
  api(project(":zygarde-webmvc"))
  api("org.springframework.boot:spring-boot-starter-security")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation(project(":zygarde-test"))
  kapt("org.springframework.boot:spring-boot-configuration-processor")
}

tasks.getByName("bootJar").enabled = false
tasks.getByName("jar").enabled = true
tasks.getByName("printCoverage").enabled = false
