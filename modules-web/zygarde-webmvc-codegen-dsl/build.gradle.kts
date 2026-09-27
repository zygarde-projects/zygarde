apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")

dependencies {
  implementation(platform(project(":zygarde-bom-codegen")))
  implementation(project(":zygarde-core"))
  implementation(project(":zygarde-webmvc"))
  implementation(project(":zygarde-web-codegen"))
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("org.springframework.boot:spring-boot-starter-webmvc")
  implementation("io.github.classgraph:classgraph")
  implementation("com.squareup:kotlinpoet")
  implementation("com.squareup:kotlinpoet-metadata")

  testImplementation(platform(project(":zygarde-bom-codegen-test")))
  testImplementation("dev.zacsweers.kctfork:core")
  testImplementation("org.jetbrains.kotlin:kotlin-compiler-embeddable")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.getByName("bootJar").enabled = false
tasks.getByName("jar").enabled = true
