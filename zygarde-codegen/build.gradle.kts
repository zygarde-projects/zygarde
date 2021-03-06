apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")

dependencies {
  implementation(project(":zygarde"))
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
  implementation("com.squareup:kotlinpoet:1.5.0")
  implementation("com.google.auto.service:auto-service:1.0-rc6")
  implementation("io.swagger:swagger-annotations:1.6.0")
  kapt("com.google.auto.service:auto-service:1.0-rc6")
  testApi("com.github.tschuchortdev:kotlin-compile-testing:1.2.9")
  testApi("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.3.72")
  testApi("org.springframework.boot:spring-boot-starter-test") {
    exclude(group = "junit")
    exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
  }
}

tasks.getByName("bootJar").enabled = false
tasks.getByName("jar").enabled = true
