rootProject.name = "zygarde"

include("zygarde")
include("zygarde-codegen")
include("zygarde-codegen-dsl")
include("zygarde-extensions-kotlinpoet")
include("zygarde-test")
include("v2-sample-core")
include("v2-sample-codegen")

project(":v2-sample-core").setProjectDir(
  File("samples/v2-dsl-sample/sample-core")
)
project(":v2-sample-codegen").setProjectDir(
  File("samples/v2-dsl-sample/sample-codegen")
)
