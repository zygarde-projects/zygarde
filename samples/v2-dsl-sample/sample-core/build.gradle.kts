apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")
apply(plugin = "org.jetbrains.kotlin.plugin.spring")

dependencies {
  kapt(project(":zygarde-codegen"))
  implementation(project(":zygarde"))
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
}

kapt {
  arguments {
  }
}

tasks.getByName("bintrayUpload").enabled = false
tasks.getByName("publish").enabled = false
tasks.getByName("printCoverage").enabled = false
tasks.getByName("bootJar").enabled = false
