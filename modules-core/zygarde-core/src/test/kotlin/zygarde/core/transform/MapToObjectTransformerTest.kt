package zygarde.core.transform

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MapToObjectTransformerTest {

  enum class Status {
    ON, OFF
  }

  interface TestInterface {
    val id: Int
    val name: String
    val status: Status?
  }

  interface TestInterfaceVar {
    var id: Int
    var name: String
  }

  abstract class AbstractTestClz {
    abstract val id: Int
    abstract val name: String
  }

  data class TestDataClz(
    override val id: Int,
    override val name: String
  ) : AbstractTestClz() {
    val status: Status? = null
  }

  class TestClz(
    val id: Int,
    val status: Status?,
  ) {
    var name: String = ""
  }

  private val inputMap = mapOf(
    "id" to 1,
    "name" to "test",
    "status" to "ON"
  )

  @Test
  fun `transform to interface`() {
    val obj = MapToObjectTransformer(TestInterface::class).transform(inputMap)
    obj.id shouldBe 1
    obj.name shouldBe "test"
    obj.status shouldBe Status.ON
  }

  @Test
  fun `transform to interface var`() {
    val obj = MapToObjectTransformer(TestInterfaceVar::class).transform(inputMap)
    obj.id shouldBe 1
    obj.name shouldBe "test"
  }

  @Test
  fun `transform to abstract class`() {
    assertThrows<IllegalArgumentException> { MapToObjectTransformer(AbstractTestClz::class).transform(inputMap) }
      .message shouldEndWith "is not an interface"
  }

  @Test
  fun `transform to data class`() {
    val obj = MapToObjectTransformer(TestDataClz::class).transform(inputMap)
    obj.id shouldBe 1
    obj.name shouldBe "test"
    obj.status shouldBe Status.ON
  }

  @Test
  fun `transform to class`() {
    val obj = MapToObjectTransformer(TestClz::class).transform(
      buildMap {
        putAll(inputMap)
        put("status", "OFF")
      }
    )
    obj.id shouldBe 1
    obj.name shouldBe "test"
    obj.status shouldBe Status.OFF
  }

  @Test
  fun `transform to class with null prop`() {
    val obj = MapToObjectTransformer(TestClz::class).transform(
      buildMap {
        putAll(inputMap)
        put("status", null)
      }
    )
    obj.id shouldBe 1
    obj.name shouldBe "test"
    obj.status shouldBe null
  }
}
