apply(plugin = "org.springframework.boot")
apply(plugin = "org.jetbrains.kotlin.plugin.spring")

dependencies {
  api("org.springframework.boot:spring-boot-starter-webmvc")
  api(project(":zygarde-di"))
  api(project(":zygarde-jackson"))
  api(project(":zygarde-web"))
  testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
  testImplementation("org.springframework.boot:spring-boot-resttestclient")
  testImplementation("org.springframework.boot:spring-boot-restclient")
  testImplementation("org.springdoc:springdoc-openapi-starter-webmvc-api:3.1.1")
  testImplementation(project(":zygarde-test"))
  kapt("org.springframework.boot:spring-boot-configuration-processor")
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
}

tasks.getByName("bootJar").enabled = false
tasks.getByName("jar").enabled = true
