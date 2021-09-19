apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")
apply(plugin = "org.jetbrains.kotlin.plugin.spring")

dependencies {
  api(project(":zygarde-jpa"))
  api(project(":zygarde-core"))
  api("org.springframework.boot:spring-boot-starter-data-jpa")
  api("org.springframework.boot:spring-boot-starter-json")
  api("org.springframework.boot:spring-boot-starter-validation")
  api("org.springframework.boot:spring-boot-starter-mail")
  api("org.springframework.boot:spring-boot-starter-thymeleaf")
  api("org.springframework.boot:spring-boot-starter-security")
  testImplementation("com.h2database:h2")
  testImplementation("org.jeasy:easy-random-core:4.2.0")
  testImplementation("org.springframework.boot:spring-boot-starter-test") {
    exclude(group = "junit")
    exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
  }
  testApi(project(":zygarde-test")) {
    exclude(group = "junit")
    exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
  }
  kapt("org.springframework.boot:spring-boot-configuration-processor")
}

tasks.getByName("printCoverage").enabled = false
tasks.getByName("bootJar").enabled = false
tasks.getByName("jar").enabled = true
