package zygarde.core.transform

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MapToObjectTransformerTest {

  interface TestInterface {
    val id: Int
    val name: String
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
  ) : AbstractTestClz()

  class TestClz(
    val id: Int,
  ) {
    var name: String = ""
  }

  private val inputMap = mapOf(
    "id" to 1,
    "name" to "test"
  )

  @Test
  fun `transform to interface`() {
    val obj = MapToObjectTransformer(TestInterface::class).transform(inputMap)
    obj.id shouldBe 1
    obj.name shouldBe "test"
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
  }

  @Test
  fun `transform to class`() {
    val obj = MapToObjectTransformer(TestClz::class).transform(inputMap)
    obj.id shouldBe 1
    obj.name shouldBe "test"
  }
}
