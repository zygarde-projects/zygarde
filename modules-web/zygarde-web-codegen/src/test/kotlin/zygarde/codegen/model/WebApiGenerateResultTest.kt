package zygarde.codegen.model

import com.squareup.kotlinpoet.FileSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class WebApiGenerateResultTest {
  @Test
  fun `should create empty WebApiGenerateResult`() {
    // given & when
    val result = WebApiGenerateResult(
      apiInterfaces = emptyList(),
      feignApiInterfaces = emptyList(),
      controllers = emptyList(),
      serviceInterfaces = emptyList()
    )

    // then
    result.apiInterfaces.shouldBeEmpty()
    result.feignApiInterfaces.shouldBeEmpty()
    result.controllers.shouldBeEmpty()
    result.serviceInterfaces.shouldBeEmpty()
  }

  @Test
  fun `should create WebApiGenerateResult with files`() {
    // given
    val apiInterface = FileSpec.builder("com.example.api", "UserApi").build()
    val feignInterface = FileSpec.builder("com.example.api.feign", "UserFeignApi").build()
    val controller = FileSpec.builder("com.example.controller", "UserController").build()
    val serviceInterface = FileSpec.builder("com.example.service", "UserService").build()

    // when
    val result = WebApiGenerateResult(
      apiInterfaces = listOf(apiInterface),
      feignApiInterfaces = listOf(feignInterface),
      controllers = listOf(controller),
      serviceInterfaces = listOf(serviceInterface)
    )

    // then
    result.apiInterfaces shouldHaveSize 1
    result.apiInterfaces[0].name shouldBe "UserApi"
    result.feignApiInterfaces shouldHaveSize 1
    result.feignApiInterfaces[0].name shouldBe "UserFeignApi"
    result.controllers shouldHaveSize 1
    result.controllers[0].name shouldBe "UserController"
    result.serviceInterfaces shouldHaveSize 1
    result.serviceInterfaces[0].name shouldBe "UserService"
  }

  @Test
  fun `should support multiple files per type`() {
    // given
    val api1 = FileSpec.builder("com.example.api", "UserApi").build()
    val api2 = FileSpec.builder("com.example.api", "TodoApi").build()
    val controller1 = FileSpec.builder("com.example.controller", "UserController").build()
    val controller2 = FileSpec.builder("com.example.controller", "TodoController").build()

    // when
    val result = WebApiGenerateResult(
      apiInterfaces = listOf(api1, api2),
      feignApiInterfaces = emptyList(),
      controllers = listOf(controller1, controller2),
      serviceInterfaces = emptyList()
    )

    // then
    result.apiInterfaces shouldHaveSize 2
    result.controllers shouldHaveSize 2
    result.apiInterfaces.map { it.name } shouldBe listOf("UserApi", "TodoApi")
    result.controllers.map { it.name } shouldBe listOf("UserController", "TodoController")
  }
}
