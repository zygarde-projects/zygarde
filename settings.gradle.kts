rootProject.name = "zygarde"

include("zygarde")
include("zygarde-core")
include("zygarde-extensions-kotlinpoet")
include("zygarde-jpa")
include("zygarde-model-mapping")
include("zygarde-web")
include("zygarde-webmvc")
include("zygarde-test")

include("zygarde-codegen")
include("zygarde-codegen-base")
include("zygarde-codegen-dsl")
include("zygarde-codegen-jpa")
include("zygarde-codegen-model-mapping")
include("zygarde-codegen-webmvc")

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
