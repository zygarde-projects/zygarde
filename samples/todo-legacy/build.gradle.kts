apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")
apply(plugin = "org.jetbrains.kotlin.plugin.spring")

dependencies {
  implementation(project(":zygarde-jpa"))
  kapt(project(":zygarde-jpa-codegen"))
  implementation(project(":zygarde-model-mapping"))
  kapt(project(":zygarde-model-mapping-codegen"))
  implementation(project(":zygarde-webmvc"))
  kapt(project(":zygarde-webmvc-codegen"))
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
}

tasks.getByName("bootJar").enabled = false
tasks.getByName("jar").enabled = true
tasks.getByName("printCoverage").enabled = false
