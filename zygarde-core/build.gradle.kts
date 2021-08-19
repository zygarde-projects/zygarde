apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")
apply(plugin = "org.jetbrains.kotlin.plugin.spring")

dependencies {
  testImplementation("org.springframework.boot:spring-boot-starter-test") {
    exclude(group = "junit")
    exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
  }
  kapt("org.springframework.boot:spring-boot-configuration-processor")
}

tasks.getByName("bootJar").enabled = false
tasks.getByName("printCoverage").enabled = false
tasks.getByName("jar").enabled = true