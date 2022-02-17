apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")

dependencies {
  implementation(platform(project(":zygarde-bom-codegen")))
  implementation(project(":zygarde-core"))
  implementation(project(":zygarde-web-codegen"))
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("io.github.classgraph:classgraph")
  implementation("com.squareup:kotlinpoet")
  implementation("com.squareup:kotlinpoet-metadata")

  testImplementation(platform(project(":zygarde-bom-codegen-test")))
  testImplementation("com.github.tschuchortdev:kotlin-compile-testing")
  testImplementation("org.jetbrains.kotlin:kotlin-compiler-embeddable")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.getByName("bootJar").enabled = false
tasks.getByName("jar").enabled = true
