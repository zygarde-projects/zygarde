apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")

dependencies {
  api(project(":zygarde-jpa"))
  implementation(project(":zygarde-extensions-kotlinpoet"))
  implementation(project(":zygarde-codegen-base"))
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("com.squareup:kotlinpoet:1.9.0")
  implementation("com.squareup:kotlinpoet-metadata:1.9.0")
  implementation("com.google.auto.service:auto-service:1.0")
  kapt("com.google.auto.service:auto-service:1.0")
  testApi("com.github.tschuchortdev:kotlin-compile-testing:1.3.6")
  testApi("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.4.30")
  testApi("org.springframework.boot:spring-boot-starter-test") {
    exclude(group = "junit")
    exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
  }
}

tasks.getByName("bootJar").enabled = false
tasks.getByName("printCoverage").enabled = false
tasks.getByName("jar").enabled = true
