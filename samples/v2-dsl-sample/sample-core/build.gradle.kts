apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")
apply(plugin = "org.jetbrains.kotlin.plugin.spring")

dependencies {
  kapt(project(":zygarde-codegen"))
  implementation(project(":zygarde-jpa"))
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
}

kapt {
  arguments {
    arg("zygarde.codegen.meta.target.folder", project(":v2-sample-model-meta").file("src/main/kotlin").absolutePath)
  }
}

tasks.getByName("bintrayUpload").enabled = false
tasks.getByName("bootJar").enabled = false
tasks.getByName("jar").enabled = true
tasks.getByName("printCoverage").enabled = false
