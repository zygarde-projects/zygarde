apply(plugin = "application")
apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")

dependencies {
  implementation(project(":todo-dsl-generated-model-mapping"))
  implementation(project(":zygarde-webmvc-codegen-dsl"))
}
tasks.getByName("bootJar").enabled = false
tasks.getByName("jar").enabled = true
tasks.getByName("printCoverage").enabled = false

tasks.getByName("run").dependsOn ":todo-codegen-dsl-models:run"

configure<JavaApplication> {
  mainClass.set("zygarde.codegen.dsl.webmvc.WebMvcDslCodegenMainKt")
  applicationDefaultJvmArgs = listOf(
    "-Dzygarde.codegen.dsl.webmvc.api-interface.package=example.api",
    "-Dzygarde.codegen.dsl.webmvc.controller.package=example.controller",
    "-Dzygarde.codegen.dsl.webmvc.service-interface.package=example.service",
    "-Dzygarde.codegen.dsl.webmvc.api-interface.write-to=${project(":todo-dsl-generated-api-interface").file("src/main/kotlin").absolutePath}",
    "-Dzygarde.codegen.dsl.webmvc.feign-interface.write-to=${project(":todo-dsl-generated-feign").file("src/main/kotlin").absolutePath}",
    "-Dzygarde.codegen.dsl.webmvc.controller.write-to=${project(":todo-dsl-generated-controller").file("src/main/kotlin").absolutePath}",
    "-Dzygarde.codegen.dsl.webmvc.service-interface.write-to=${project(":todo-dsl-generated-service-interface").file("src/main/kotlin").absolutePath}"
  )
}
