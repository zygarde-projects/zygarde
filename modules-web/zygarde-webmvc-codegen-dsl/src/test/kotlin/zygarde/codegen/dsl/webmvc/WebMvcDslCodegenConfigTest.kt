package zygarde.codegen.dsl.webmvc

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class WebMvcDslCodegenConfigTest {
  @Test
  fun `should create config with all required fields`() {
    // when
    val config = WebMvcDslCodegenConfig(
      apiInterfacePackage = "com.example.api",
      controllerPackage = "com.example.controller",
      serviceInterfacePackage = "com.example.service"
    )

    // then
    config.apiInterfacePackage shouldBe "com.example.api"
    config.controllerPackage shouldBe "com.example.controller"
    config.serviceInterfacePackage shouldBe "com.example.service"
  }

  @Test
  fun `should support copy with different values`() {
    // given
    val config = WebMvcDslCodegenConfig(
      apiInterfacePackage = "com.example.api",
      controllerPackage = "com.example.controller",
      serviceInterfacePackage = "com.example.service"
    )

    // when
    val copied = config.copy(apiInterfacePackage = "com.other.api")

    // then
    copied.apiInterfacePackage shouldBe "com.other.api"
    copied.controllerPackage shouldBe "com.example.controller"
    copied.serviceInterfacePackage shouldBe "com.example.service"
  }

  @Test
  fun `should support equality comparison`() {
    // given
    val config1 = WebMvcDslCodegenConfig(
      apiInterfacePackage = "com.example.api",
      controllerPackage = "com.example.controller",
      serviceInterfacePackage = "com.example.service"
    )
    val config2 = WebMvcDslCodegenConfig(
      apiInterfacePackage = "com.example.api",
      controllerPackage = "com.example.controller",
      serviceInterfacePackage = "com.example.service"
    )

    // then
    config1 shouldBe config2
  }

  @Test
  fun `should have proper hashCode`() {
    // given
    val config1 = WebMvcDslCodegenConfig(
      apiInterfacePackage = "com.example.api",
      controllerPackage = "com.example.controller",
      serviceInterfacePackage = "com.example.service"
    )
    val config2 = WebMvcDslCodegenConfig(
      apiInterfacePackage = "com.example.api",
      controllerPackage = "com.example.controller",
      serviceInterfacePackage = "com.example.service"
    )

    // then
    config1.hashCode() shouldBe config2.hashCode()
  }
}
