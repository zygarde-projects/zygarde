rootProject.name = "zygarde"

fun registerModules(path: String) {
  File(rootProject.projectDir, path)
    .listFiles { f -> f.isDirectory && File(f, "build.gradle.kts").exists() }
    ?.forEach { f ->
      include(f.name)
      project(":${f.name}").projectDir = f
    }
}

registerModules("modules-core")
registerModules("modules-jpa")
registerModules("modules-web")
registerModules("modules-model-mapping")
registerModules("modules-codegen-support")
registerModules("modules-test-support")
registerModules("modules-bom")

if (System.getenv("JITPACK") != "true") {
  registerModules("samples")
  registerModules("samples/todo-multimodule-dsl")
}

