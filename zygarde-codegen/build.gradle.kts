apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")

dependencies {
  implementation(project(":zygarde"))
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("com.squareup:kotlinpoet:1.5.0")
  implementation("com.google.auto.service:auto-service:1.0-rc6")
  kapt("com.google.auto.service:auto-service:1.0-rc6")
  testApi("com.github.tschuchortdev:kotlin-compile-testing:1.2.6")
  testApi("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.3.61")
}

tasks.getByName("bootJar").enabled = false
tasks.getByName("jar").enabled = true
tasks.getByName("printCoverage").enabled = false
