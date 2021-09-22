apply(plugin = "org.springframework.boot")
apply(plugin = "org.jetbrains.kotlin.plugin.spring")

dependencies {
  api("org.springframework.boot:spring-boot-starter-thymeleaf")
  api(project(":zygarde-core"))
  testImplementation(project(":zygarde-test"))
  kapt("org.springframework.boot:spring-boot-configuration-processor")
}

tasks.getByName("printCoverage").enabled = false
tasks.getByName("bootJar").enabled = false
tasks.getByName("jar").enabled = true
