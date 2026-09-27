apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")

dependencies {
  implementation(platform(project(":zygarde-bom-codegen")))
  implementation("com.squareup:kotlinpoet")
  implementation("com.squareup:kotlinpoet-ksp:1.18.1")
  implementation("com.google.devtools.ksp:symbol-processing-api:2.2.20-2.0.4")
}

tasks.getByName("bootJar").enabled = false
tasks.getByName("jar").enabled = true
