apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")

dependencies {
  api(project(":zygarde-webmvc"))
  implementation(project(":zygarde-core"))
  implementation(project(":zygarde-codegen-base"))
  implementation("org.springframework.boot:spring-boot-starter-webmvc")
  implementation("org.springframework.boot:spring-boot-starter-graphql")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("org.springframework.cloud:spring-cloud-starter-openfeign")

  implementation(platform(project(":zygarde-bom-codegen")))
  implementation("com.squareup:kotlinpoet")
  implementation("com.squareup:kotlinpoet-metadata")

  testImplementation(platform(project(":zygarde-bom-codegen-test")))
  testImplementation("dev.zacsweers.kctfork:core")
  testImplementation("org.jetbrains.kotlin:kotlin-compiler-embeddable")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.getByName("bootJar").enabled = false
tasks.getByName("jar").enabled = true
