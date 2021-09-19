rootProject.name = "zygarde"

fun registerModules(path: String) {
  File(rootProject.projectDir, path)
    .listFiles { f -> f.isDirectory && f.name.startsWith("zygarde-") }
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

include("zygarde")

include("v2-sample-core")
include("v2-sample-model-meta")
include("v2-sample-codegen")
include("v2-sample-codegen-generated")
include("v2-jpa-sample-core")

project(":v2-sample-core").setProjectDir(
  File("samples/v2-dsl-sample/sample-core")
)
project(":v2-sample-model-meta").setProjectDir(
  File("samples/v2-dsl-sample/sample-model-meta")
)
project(":v2-sample-codegen").setProjectDir(
  File("samples/v2-dsl-sample/sample-codegen")
)
project(":v2-sample-codegen-generated").setProjectDir(
  File("samples/v2-dsl-sample/sample-codegen-generated")
)
project(":v2-jpa-sample-core").setProjectDir(
  File("samples/v2-jpa-sample/jpa-sample-core")
)
