package zygarde.codegen.dsl.webmvc

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class WebMvcDslCodegenExtendedTest {

  data class TestDto(val value: String)

  @AfterEach
  fun cleanup() {
    // Clean up system properties after each test
    System.clearProperty("zygarde.codegen.dsl.webmvc.api-interface.package")
    System.clearProperty("zygarde.codegen.dsl.webmvc.controller.package")
    System.clearProperty("zygarde.codegen.dsl.webmvc.service-interface.package")
  }

  @Test
  fun `should use system properties for config packages`() {
    // given
    System.setProperty("zygarde.codegen.dsl.webmvc.api-interface.package", "custom.api")
    System.setProperty("zygarde.codegen.dsl.webmvc.controller.package", "custom.controller")
    System.setProperty("zygarde.codegen.dsl.webmvc.service-interface.package", "custom.service")

    val codegen = object : WebMvcDslCodegen() {
      override fun codegen() {
        api("TestApi", "/test") {
          get("test", "/test") {}
        }
      }
    }

    // when
    codegen.codegen()

    // then
    val apis = codegen.apisToGenerate
    apis shouldHaveSize 1
    apis[0].apiInterfacePackage shouldBe "custom.api"
    apis[0].controllerPackage shouldBe "custom.controller"
    apis[0].serviceInterfacePackage shouldBe "custom.service"
  }

  @Test
  fun `should use default packages when system properties not set`() {
    // given
    val codegen = object : WebMvcDslCodegen() {
      override fun codegen() {
        api("TestApi", "/test") {
          get("test", "/test") {}
        }
      }
    }

    // when
    codegen.codegen()

    // then
    val apis = codegen.apisToGenerate
    apis shouldHaveSize 1
    apis[0].apiInterfacePackage shouldBe "zygarde.generated.api"
    apis[0].controllerPackage shouldBe "zygarde.generated.api.impl"
    apis[0].serviceInterfacePackage shouldBe "zygarde.generated.service"
  }

  @Test
  fun `should support api with 2 parameters without basePath`() {
    // given
    val codegen = object : WebMvcDslCodegen() {
      override fun codegen() {
        api("TestApi") {
          get("test", "/test") {
            res<TestDto>()
          }
        }
      }
    }

    // when
    codegen.codegen()

    // then
    val apis = codegen.apisToGenerate
    apis shouldHaveSize 1
    apis[0].apiName shouldBe "TestApi"
    apis[0].basePath shouldBe null
  }

  @Test
  fun `should support api with 3 parameters with basePath`() {
    // given
    val codegen = object : WebMvcDslCodegen() {
      override fun codegen() {
        api("TestApi", "/api/test") {
          get("test", "/test") {
            res<TestDto>()
          }
        }
      }
    }

    // when
    codegen.codegen()

    // then
    val apis = codegen.apisToGenerate
    apis shouldHaveSize 1
    apis[0].apiName shouldBe "TestApi"
    apis[0].basePath shouldBe "/api/test"
  }

  @Test
  fun `should accumulate multiple apis`() {
    // given
    val codegen = object : WebMvcDslCodegen() {
      override fun codegen() {
        api("Api1", "/api1") {
          get("test1", "/test1") {}
        }
        api("Api2", "/api2") {
          get("test2", "/test2") {}
        }
        api("Api3") {
          get("test3", "/test3") {}
        }
      }
    }

    // when
    codegen.codegen()

    // then
    val apis = codegen.apisToGenerate
    apis shouldHaveSize 3
    apis[0].apiName shouldBe "Api1"
    apis[1].apiName shouldBe "Api2"
    apis[2].apiName shouldBe "Api3"
  }

  @Test
  fun `should handle empty codegen implementation`() {
    // given
    val codegen = object : WebMvcDslCodegen() {
      override fun codegen() {
        // Empty implementation
      }
    }

    // when
    codegen.codegen()

    // then
    codegen.apisToGenerate shouldHaveSize 0
  }

  @Test
  fun `should support null basePath in 3-parameter api`() {
    // given
    val codegen = object : WebMvcDslCodegen() {
      override fun codegen() {
        api("TestApi", null) {
          get("test", "/test") {}
        }
      }
    }

    // when
    codegen.codegen()

    // then
    val apis = codegen.apisToGenerate
    apis shouldHaveSize 1
    apis[0].basePath shouldBe null
  }
}
