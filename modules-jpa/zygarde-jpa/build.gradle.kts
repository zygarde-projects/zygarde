apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")
apply(plugin = "org.jetbrains.kotlin.plugin.spring")

dependencies {
  api(project(":zygarde-core"))
  api(project(":zygarde-jackson"))
  api("org.springframework.boot:spring-boot-starter-data-jpa")
  testImplementation("org.jeasy:easy-random-core:4.2.0")
  testImplementation("com.h2database:h2")
  testImplementation(project(":zygarde-test"))
  testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.getByName("bootJar").enabled = false
tasks.getByName("jar").enabled = true
