apply(plugin = "application")
apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")

dependencies {
  implementation(project(":todo-src-core"))
  implementation(project(":todo-kapt-generated-model-meta"))
  implementation(project(":zygarde-model-mapping"))
  implementation(project(":zygarde-model-mapping-codegen-dsl"))
}
tasks.getByName("bootJar").enabled = false
tasks.getByName("jar").enabled = true
tasks.getByName("printCoverage").enabled = false

tasks.getByName("run").dependsOn ":todo-src-core:kaptkotlin"

configure<JavaApplication> {
  mainClass.set("zygarde.codegen.dsl.ModelMappingDslMainKt")
  applicationDefaultJvmArgs = listOf(
    "-Dzygarde.codegen.dsl.model-mapping.write-to=${project(":todo-dsl-generated-model-mapping").file("src/main/kotlin").absolutePath}",
    "-Dzygarde.codegen.dsl.dto.write-to=${project(":todo-dsl-generated-dto").file("src/main/kotlin").absolutePath}"
  )
}
