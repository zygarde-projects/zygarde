apply(plugin = "application")
apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")

dependencies {
  implementation(platform(project(":zygarde-bom-codegen")))
  implementation(project(":zygarde-sql-api"))
  implementation(project(":zygarde-web-codegen"))
  implementation(project(":zygarde-webmvc"))
  implementation("io.github.classgraph:classgraph")
  implementation("com.squareup:kotlinpoet")
  api("com.github.jsqlparser:jsqlparser:5.0")
  implementation("org.springframework.boot:spring-boot-starter-web")

  testImplementation(platform(project(":zygarde-bom-codegen-test")))
  testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.getByName("bootJar").enabled = false
tasks.getByName("jar").enabled = true

configure<JavaApplication> {
  mainClass.set("zygarde.codegen.dsl.sqlapi.SqlApiDslCodegenMainKt")
}
