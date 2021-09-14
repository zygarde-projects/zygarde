apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")
apply(plugin = "org.jetbrains.kotlin.plugin.spring")

dependencies {
  kapt(project(":zygarde-codegen-jpa"))
  implementation(project(":zygarde"))
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
}

tasks.getByName("bintrayUpload").enabled = false
tasks.getByName("bootJar").enabled = false
tasks.getByName("jar").enabled = true
tasks.getByName("printCoverage").enabled = false
