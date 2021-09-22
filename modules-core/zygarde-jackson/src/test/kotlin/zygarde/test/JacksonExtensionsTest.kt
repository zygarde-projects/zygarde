package zygarde.test

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import zygarde.json.jsonStringToList
import zygarde.json.jsonStringToMap
import zygarde.json.jsonStringToObject
import zygarde.json.toJsonString

data class Foo(val foo: String)

data class Bar(
  val id: String,
  val items: List<BarItem>
)

data class BarItem(
  val id: String,
  val name: String
)

open class GenericFoo<T>(
  val t: T
)

class GFoo(t: Foo) : GenericFoo<Foo>(t)

/**
 * @author leo
 */
class JsonExtensionsTest {
  @Test
  fun `should able to convert anything to json`() {
    mapOf("foo" to "bar").toJsonString() shouldBe """{"foo":"bar"}"""
  }

  @Test
  fun `should able to convert json string to object`() {
    "{\"foo\":\"bar\"}".jsonStringToObject<Foo>().foo shouldBe "bar"
    Bar(
      id = "123",
      items = listOf(BarItem(id = "AAA", name = "BBB"))
    ).toJsonString().jsonStringToObject<Bar>().id shouldBe "123"
  }

  @Test
  fun `should able to convert json string to list`() {
    """["a", "b", "c"]""".jsonStringToList<String>().size shouldBe 3
  }

  @Test
  fun `should able to convert json string to list with class`() {
    """[{"foo":"bar"}]""".jsonStringToList(Foo::class).first().foo shouldBe "bar"
  }

  @Test
  fun `should able to convert json string to map`() {
    """{"foo":"bar"}""".jsonStringToMap<String, String>()["foo"] shouldBe "bar"
  }

  @Test
  fun `should able to convert json string to map with class`() {
    """{"foo": 1}""".jsonStringToMap<String, Number>()["foo"] shouldBe 1
  }

  @Test
  fun `should able to convert generic`() {
    val json = GFoo(t = Foo("generic")).toJsonString()
    json shouldBe """{"t":{"foo":"generic"}}"""
    val foo = json.jsonStringToObject(GFoo::class)
    foo.t shouldBe Foo("generic")
  }
}
