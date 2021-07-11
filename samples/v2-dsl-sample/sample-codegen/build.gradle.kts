apply(plugin = "application")
apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")

dependencies {
  implementation(project(":v2-sample-core"))
  implementation(project(":v2-sample-model-meta"))
  implementation(project(":zygarde"))
  implementation(project(":zygarde-codegen-dsl"))
  kapt(project(":zygarde-codegen"))
}
tasks.getByName("bintrayUpload").enabled = false
tasks.getByName("bootJar").enabled = false
tasks.getByName("jar").enabled = true
tasks.getByName("printCoverage").enabled = false

tasks.getByName("run").dependsOn ":v2-sample-core:kaptkotlin"

configure<JavaApplication> {
  mainClass.set("zygarde.codegen.dsl.DslMainKt")
  applicationDefaultJvmArgs = listOf(
    "-Dzygarde.codegen.target=${project(":v2-sample-codegen-generated").file("src/main/kotlin").absolutePath}"
  )
}
