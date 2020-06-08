apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")
apply(plugin = "org.jetbrains.kotlin.plugin.spring")

dependencies {
  api("org.springframework.boot:spring-boot-starter-data-jpa")
  api("org.springframework.boot:spring-boot-starter-json")
  api("com.fasterxml.jackson.module:jackson-module-kotlin:2.10.2")
  api("io.swagger:swagger-annotations:1.6.0")
  api("io.springfox:springfox-swagger2:2.9.2")
  testImplementation("com.h2database:h2")
  testImplementation("org.jeasy:easy-random-core:4.2.0")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testApi(project(":zygarde-test"))
}

tasks.getByName("bootJar").enabled = false
tasks.getByName("jar").enabled = true
