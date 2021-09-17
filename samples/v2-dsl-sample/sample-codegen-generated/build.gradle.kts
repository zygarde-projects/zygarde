apply(plugin = "application")
apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")

dependencies {
  implementation(project(":v2-sample-core"))
  implementation(project(":zygarde-jpa"))
  implementation(project(":zygarde-model-mapping"))
  implementation(project(":zygarde-webmvc"))
  implementation("org.springframework.boot:spring-boot-starter-validation")
}
tasks.getByName("bintrayUpload").enabled = false
tasks.getByName("bootJar").enabled = false
tasks.getByName("jar").enabled = true
tasks.getByName("printCoverage").enabled = false
tasks.getByName("ktlintMainSourceSetCheck").enabled = false
