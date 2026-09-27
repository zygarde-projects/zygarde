apply(plugin = "org.springframework.boot")
apply(plugin = "org.jetbrains.kotlin.plugin.spring")

dependencies {
  api("org.springframework.boot:spring-boot-starter")
  api("org.springframework:spring-aop")
  api("org.springframework.retry:spring-retry:2.0.13")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.getByName("bootJar").enabled = false
tasks.getByName("jar").enabled = true
