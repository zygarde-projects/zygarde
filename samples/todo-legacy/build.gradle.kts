apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")
apply(plugin = "org.jetbrains.kotlin.plugin.spring")

dependencies {
  kapt(project(":zygarde-jpa-codegen"))
  kapt(project(":zygarde-model-mapping-codegen"))
  kapt(project(":zygarde-webmvc-codegen"))
  implementation(project(":zygarde-jpa"))
  implementation(project(":zygarde-model-mapping"))
  implementation(project(":zygarde-webmvc"))
  implementation(project(":zygarde-webmvc-security"))
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
}

tasks.getByName("bootJar").enabled = false
tasks.getByName("jar").enabled = true
tasks.getByName("printCoverage").enabled = false

// kapt {
//   val apiGenFile = File(".api-gen.json")
//   apiGenFile.writeText(
//
//     """
// {
//   "todo-api": {
// "generateApiInterfacesTo":  "${projectDir.absolutePath}/generated/api-interfaces",
// "generateFeignApiInterfacesTo": "${projectDir.absolutePath}/generated/api-feign",
// "generateControllersTo":  "${projectDir.absolutePath}/generated/controllers",
// "generateServiceInterfacesTo": "${projectDir.absolutePath}/generated/services"
//   }
// }
//     """.trimIndent()
//   )
//
//   arguments {
//     arg("zygarde.webmvc_codegen.grouped_api_config_json", apiGenFile.absolutePath)
//   }
// }
