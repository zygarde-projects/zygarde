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
  implementation("com.google.devtools.ksp:symbol-processing-api:1.9.25-1.0.20")

  testImplementation(platform(project(":zygarde-bom-codegen-test")))
  testImplementation("com.github.tschuchortdev:kotlin-compile-testing-ksp:1.6.0")
  testImplementation("org.jetbrains.kotlin:kotlin-compiler-embeddable")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.getByName("bootJar").enabled = false
tasks.getByName("jar").enabled = true
