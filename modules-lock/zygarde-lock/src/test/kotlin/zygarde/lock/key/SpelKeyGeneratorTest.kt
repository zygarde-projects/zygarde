package zygarde.lock.key

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.core.convert.support.DefaultConversionService
import zygarde.lock.exception.EvaluationConvertException

class SpelKeyGeneratorTest {
  private val generator = SpelKeyGenerator(DefaultConversionService.getSharedInstance())

  @Test
  fun `should resolve key using executionPath`() {
    val method = SampleService::class.java.getMethod("doSomething", String::class.java)
    val obj = SampleService()

    val keys = generator.resolveKeys("", "#executionPath", obj, method, arrayOf("test"))

    keys shouldNotBe null
    keys.size shouldBe 1
    keys[0] shouldBe "${SampleService::class.java.canonicalName}.doSomething"
  }

  @Test
  fun `should resolve key from method argument`() {
    val method = SampleService::class.java.getMethod("doSomething", String::class.java)
    val obj = SampleService()

    val keys = generator.resolveKeys("", "#p0", obj, method, arrayOf("myKey"))

    keys shouldNotBe null
    keys.size shouldBe 1
    keys[0] shouldBe "myKey"
  }

  @Test
  fun `should prepend prefix to resolved keys`() {
    val method = SampleService::class.java.getMethod("doSomething", String::class.java)
    val obj = SampleService()

    val keys = generator.resolveKeys("lock:", "#p0", obj, method, arrayOf("myKey"))

    keys.size shouldBe 1
    keys[0] shouldBe "lock:myKey"
  }

  @Test
  fun `should resolve multiple keys from list expression`() {
    val method = SampleService::class.java.getMethod("doSomething", String::class.java)
    val obj = SampleService()

    val keys = generator.resolveKeys("", "{'key1', 'key2'}", obj, method, arrayOf("unused"))

    keys.size shouldBe 2
    keys[0] shouldBe "key1"
    keys[1] shouldBe "key2"
  }

  @Test
  fun `should throw when expression evaluates to null`() {
    val method = SampleService::class.java.getMethod("doSomethingNullable", String::class.java)
    val obj = SampleService()

    assertThrows<EvaluationConvertException> {
      generator.resolveKeys("", "#p0", obj, method, arrayOf<Any?>(null))
    }
  }

  @Test
  fun `should work without ConversionService using default`() {
    val noArgGenerator = SpelKeyGenerator()
    val method = SampleService::class.java.getMethod("doSomething", String::class.java)
    val obj = SampleService()

    val keys = noArgGenerator.resolveKeys("", "#p0", obj, method, arrayOf("defaultKey"))

    keys.size shouldBe 1
    keys[0] shouldBe "defaultKey"
  }

  class SampleService {
    fun doSomething(key: String): String = key

    fun doSomethingNullable(key: String?): String? = key
  }
}
