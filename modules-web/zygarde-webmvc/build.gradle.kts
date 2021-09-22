apply(plugin = "org.springframework.boot")
apply(plugin = "org.jetbrains.kotlin.plugin.spring")

dependencies {
  api("org.springframework.boot:spring-boot-starter-web")
  api(project(":zygarde-di"))
  api(project(":zygarde-jackson"))
  api(project(":zygarde-web"))
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation(project(":zygarde-test"))
  kapt("org.springframework.boot:spring-boot-configuration-processor")
}

tasks.getByName("bootJar").enabled = false
tasks.getByName("jar").enabled = true
tasks.getByName("printCoverage").enabled = false
