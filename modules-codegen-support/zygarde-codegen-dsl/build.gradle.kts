apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")

dependencies {
  implementation(project(":zygarde"))
  implementation(project(":zygarde-model-mapping"))
  implementation(project(":zygarde-codegen-base"))
  implementation("io.github.classgraph:classgraph:4.8.21")
  testImplementation("commons-io:commons-io:2.2")

  implementation(platform(project(":zygarde-bom-codegen")))
  implementation("com.squareup:kotlinpoet")
  implementation("com.squareup:kotlinpoet-metadata")
  implementation("com.google.auto.service:auto-service")

  kapt(platform(project(":zygarde-bom-codegen")))
  kapt("com.google.auto.service:auto-service")

  testImplementation(platform(project(":zygarde-bom-codegen-test")))
  testImplementation("com.github.tschuchortdev:kotlin-compile-testing")
  testImplementation("org.jetbrains.kotlin:kotlin-compiler-embeddable")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.getByName("bootJar").enabled = false
tasks.getByName("printCoverage").enabled = false
tasks.getByName("jar").enabled = true