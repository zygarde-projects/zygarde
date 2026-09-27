apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")

dependencies {
  api(project(":zygarde-model-mapping"))
  api(project(":zygarde-codegen-ksp-base"))
  implementation(project(":zygarde-codegen-base"))
  implementation(project(":zygarde-model-mapping-codegen-dsl"))

  implementation(platform(project(":zygarde-bom-codegen")))
  implementation("com.squareup:kotlinpoet")
  implementation("com.squareup:kotlinpoet-ksp:1.18.1")
  implementation("com.google.devtools.ksp:symbol-processing-api:2.2.20-2.0.4")

  testImplementation(platform(project(":zygarde-bom-codegen-test")))
  testImplementation("dev.zacsweers.kctfork:ksp:0.10.1")
  testImplementation("org.jetbrains.kotlin:kotlin-compiler-embeddable")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.getByName("bootJar").enabled = false
tasks.getByName("jar").enabled = true
