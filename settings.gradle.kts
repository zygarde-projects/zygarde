rootProject.name = "zygarde"

include("zygarde")
include("zygarde-codegen")
include("zygarde-test")
include("v2-sample-core")

project(":v2-sample-core").setProjectDir(
  File("samples/v2-dsl-sample/sample-core")
)
