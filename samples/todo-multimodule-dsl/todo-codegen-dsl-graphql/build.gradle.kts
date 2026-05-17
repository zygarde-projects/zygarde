apply(plugin = "application")
apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")

dependencies {
  implementation(project(":todo-src-core"))
  implementation(project(":todo-dsl-generated-dto"))
  implementation(project(":todo-codegen-dsl-models"))
  implementation(project(":zygarde-jpa"))
  implementation(project(":zygarde-graphql-codegen-dsl"))
  // typeFrom / inputFrom reference model-mapping CodegenDto declarations directly.
  implementation(project(":zygarde-model-mapping-codegen-dsl"))
}
tasks.getByName("bootJar").enabled = false
tasks.getByName("jar").enabled = true
tasks.getByName("bootDistTar").enabled = false
tasks.getByName("bootDistZip").enabled = false
tasks.getByName("bootStartScripts").enabled = false

configure<JavaApplication> {
  val graphqlServiceInterfaceDir = project(":todo-dsl-generated-graphql-service-interface").file("src/main/kotlin").absolutePath
  val graphqlSchemaDir = project(":todo-dsl-generated-graphql-schema").file("src/main/resources/graphql").absolutePath

  mainClass.set("zygarde.codegen.dsl.graphql.GraphQlDslCodegenMainKt")
  applicationDefaultJvmArgs = listOf(
    "-Dzygarde.codegen.dsl.graphql.controller.package=example.graphql",
    "-Dzygarde.codegen.dsl.graphql.service-interface.package=example.graphql.service",
    "-Dzygarde.codegen.dsl.graphql.controller.write-to=${project(":todo-dsl-generated-graphql-controller").file("src/main/kotlin").absolutePath}",
    "-Dzygarde.codegen.dsl.graphql.service-interface.write-to=$graphqlServiceInterfaceDir",
    "-Dzygarde.codegen.dsl.graphql.schema.write-to=$graphqlSchemaDir",
  )
}
