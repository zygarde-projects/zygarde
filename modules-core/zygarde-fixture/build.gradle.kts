apply(plugin = "org.springframework.boot")
apply(plugin = "org.jetbrains.kotlin.plugin.spring")

dependencies {
  implementation(project(":zygarde-core"))
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  kapt("org.springframework.boot:spring-boot-configuration-processor")
}

tasks.getByName("printCoverage").enabled = false
tasks.getByName("bootJar").enabled = false
tasks.getByName("jar").enabled = true
