rootProject.name = "zygarde"

include("zygarde")
include("zygarde-core")
include("zygarde-jpa")
include("zygarde-codegen")
include("zygarde-codegen-base")
include("zygarde-codegen-dsl")
include("zygarde-codegen-jpa")
include("zygarde-extensions-kotlinpoet")
include("zygarde-test")
include("zygarde-webmvc")
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
