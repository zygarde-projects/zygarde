apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")

dependencies {
  implementation(project(":zygarde-webmvc"))
  implementation(project(":zygarde-core"))
  implementation(project(":zygarde-codegen-base"))
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
  testImplementation(project(":zygarde-model-mapping-codegen"))
  testImplementation(project(":zygarde-jpa-codegen"))

  implementation(platform(project(":zygarde-bom-codegen")))
  implementation("com.squareup:kotlinpoet")
  implementation("com.squareup:kotlinpoet-metadata")
  implementation("com.google.auto.service:auto-service")

  kapt(platform(project(":zygarde-bom-codegen")))
  kapt("com.google.auto.service:auto-service")

  testImplementation(platform(project(":zygarde-bom-codegen-test")))
  testImplementation(project(":zygarde-webmvc-security"))
  testImplementation("com.github.tschuchortdev:kotlin-compile-testing")
  testImplementation("org.jetbrains.kotlin:kotlin-compiler-embeddable")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.getByName("bootJar").enabled = false
tasks.getByName("jar").enabled = true
