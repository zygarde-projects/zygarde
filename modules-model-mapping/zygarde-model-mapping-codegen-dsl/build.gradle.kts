apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")

dependencies {
  implementation(platform(project(":zygarde-bom-codegen")))
  implementation(project(":zygarde-model-mapping"))
  implementation(project(":zygarde-codegen-base"))
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("io.github.classgraph:classgraph")
  implementation("com.squareup:kotlinpoet")
  implementation("com.squareup:kotlinpoet-metadata")
  implementation("com.google.auto.service:auto-service")
  implementation("org.hibernate:hibernate-core")

  kapt(platform(project(":zygarde-bom-codegen")))
  kapt("com.google.auto.service:auto-service")

  testImplementation(platform(project(":zygarde-bom-codegen-test")))
  testImplementation("commons-io:commons-io:2.2")
  testImplementation("com.github.tschuchortdev:kotlin-compile-testing")
  testImplementation("org.jetbrains.kotlin:kotlin-compiler-embeddable")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.getByName("bootJar").enabled = false
tasks.getByName("printCoverage").enabled = false
tasks.getByName("jar").enabled = true
