package zygarde.core.di

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import org.springframework.context.support.GenericApplicationContext

class DiServiceContextUnitTest {

  class TestBean

  @Test
  fun `beanMap should be mutable map`() {
    // given
    val map = DiServiceContext.beanMap

    // when - add custom entry
    val testKey = TestBean::class.java
    val testValue = TestBean()
    map[testKey] = testValue

    // then
    map[testKey] shouldBe testValue

    // cleanup
    map.remove(testKey)
  }

  @Test
  fun `setApplicationContext should set ctx`() {
    // given
    val context = GenericApplicationContext()

    // when
    DiServiceContext.setApplicationContext(context)

    // then
    DiServiceContext.ctx shouldBe context
  }

  @Test
  fun `ctx should be accessible after initialization`() {
    // when
    val ctx = DiServiceContext.ctx

    // then
    ctx shouldNotBe null
  }
}
