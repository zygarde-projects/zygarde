apply(plugin = "application")
apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")

dependencies {
  implementation(project(":todo-dsl-generated-dto"))
  implementation(project(":todo-src-core"))
  implementation(project(":zygarde-webmvc-codegen-dsl"))
}
tasks.getByName("bootJar").enabled = false
tasks.getByName("jar").enabled = true
tasks.getByName("bootDistTar").enabled = false
tasks.getByName("bootDistZip").enabled = false
tasks.getByName("bootStartScripts").enabled = false

tasks.getByName("run").dependsOn(":todo-codegen-dsl-models:run")

fun codegenWriteToArg(propertyName: String, projectName: String): String {
  return "-D$propertyName=${project(projectName).file("src/main/kotlin").absolutePath}"
}

configure<JavaApplication> {
  mainClass.set("zygarde.codegen.dsl.webmvc.WebMvcDslCodegenMainKt")
  applicationDefaultJvmArgs = listOf(
    "-Dzygarde.codegen.dsl.webmvc.api-interface.package=example.api",
    "-Dzygarde.codegen.dsl.webmvc.controller.package=example.controller",
    "-Dzygarde.codegen.dsl.webmvc.service-interface.package=example.service",
    "-Dzygarde.codegen.dsl.webmvc.service-impl.package=example.service.impl",
    codegenWriteToArg("zygarde.codegen.dsl.webmvc.api-interface.write-to", ":todo-dsl-generated-api-interface"),
    codegenWriteToArg("zygarde.codegen.dsl.webmvc.feign-interface.write-to", ":todo-dsl-generated-feign"),
    codegenWriteToArg("zygarde.codegen.dsl.webmvc.controller.write-to", ":todo-dsl-generated-controller"),
    codegenWriteToArg("zygarde.codegen.dsl.webmvc.service-interface.write-to", ":todo-dsl-generated-service-interface"),
    codegenWriteToArg("zygarde.codegen.dsl.webmvc.service-impl.write-to", ":todo-dsl-generated-service-impl"),
  )
}
