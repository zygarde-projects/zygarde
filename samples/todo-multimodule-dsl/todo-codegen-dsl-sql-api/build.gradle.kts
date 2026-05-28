apply(plugin = "application")
apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")

dependencies {
  implementation(project(":zygarde-sql-api-codegen-dsl"))
  implementation(project(":todo-src-core"))
}

tasks.getByName("bootJar").enabled = false
tasks.getByName("jar").enabled = true
tasks.getByName("bootDistTar").enabled = false
tasks.getByName("bootDistZip").enabled = false
tasks.getByName("bootStartScripts").enabled = false

val generatedDtoDir = project(":todo-dsl-generated-sql-api-dto").file("src/main/kotlin").absolutePath
val generatedApiInterfaceDir = project(":todo-dsl-generated-sql-api-interface").file("src/main/kotlin").absolutePath
val generatedFeignDir = project(":todo-dsl-generated-sql-api-feign").file("src/main/kotlin").absolutePath
val generatedControllerDir = project(":todo-dsl-generated-sql-api-controller").file("src/main/kotlin").absolutePath
val generatedServiceInterfaceDir = project(":todo-dsl-generated-sql-api-service-interface").file("src/main/kotlin").absolutePath
val generatedServiceImplDir = project(":todo-dsl-generated-sql-api-service-impl").file("src/main/kotlin").absolutePath

configure<JavaApplication> {
  mainClass.set("zygarde.codegen.dsl.sqlapi.SqlApiDslCodegenMainKt")
  applicationDefaultJvmArgs = listOf(
    "-Dzygarde.codegen.dsl.sql-api.dto.package=example.sqlapi.dto",
    "-Dzygarde.codegen.dsl.sql-api.api-interface.package=example.sqlapi.api",
    "-Dzygarde.codegen.dsl.sql-api.controller.package=example.sqlapi.controller",
    "-Dzygarde.codegen.dsl.sql-api.service-interface.package=example.sqlapi.service",
    "-Dzygarde.codegen.dsl.sql-api.service-impl.package=example.sqlapi.service.impl",
    "-Dzygarde.codegen.dsl.sql-api.dto.write-to=$generatedDtoDir",
    "-Dzygarde.codegen.dsl.sql-api.api-interface.write-to=$generatedApiInterfaceDir",
    "-Dzygarde.codegen.dsl.sql-api.feign-interface.write-to=$generatedFeignDir",
    "-Dzygarde.codegen.dsl.sql-api.controller.write-to=$generatedControllerDir",
    "-Dzygarde.codegen.dsl.sql-api.service-interface.write-to=$generatedServiceInterfaceDir",
    "-Dzygarde.codegen.dsl.sql-api.service-impl.write-to=$generatedServiceImplDir",
  )
}
