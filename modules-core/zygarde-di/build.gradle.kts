apply(plugin = "org.springframework.boot")
apply(plugin = "org.jetbrains.kotlin.plugin.spring")

dependencies {
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  kapt("org.springframework.boot:spring-boot-configuration-processor")
}

tasks.getByName("bootJar").enabled = false
tasks.getByName("jar").enabled = true
