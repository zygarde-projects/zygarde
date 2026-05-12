apply(plugin = "org.springframework.boot")

dependencies {
  testImplementation("commons-io:commons-io:2.22.0")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.getByName("bootJar").enabled = false
tasks.getByName("jar").enabled = true
