package zygarde.test

import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import zygarde.core.di.DiServiceContext
import zygarde.core.di.DiServiceContext.autowired
import zygarde.core.di.DiServiceContext.bean

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = [DiServiceContextTest.DiServiceContextTestApp::class])
class DiServiceContextTest {
  class MyBean

  class AnotherBean

  class MyBeanUser {
    val myBean: MyBean by autowired()
  }

  @SpringBootApplication
  class DiServiceContextTestApp {
    @Bean
    fun myBean() = MyBean()

    @Bean
    fun anotherBean() = AnotherBean()

    @Bean
    fun myBeanUser() = MyBeanUser()
  }

  @BeforeEach
  fun setup() {
    // Clear cache before each test to ensure clean state
    DiServiceContext.beanMap.clear()
  }

  @Test
  fun `bean and autowired`() {
    bean<MyBean>() shouldNotBe null
    bean<MyBeanUser>().myBean shouldBeSameInstanceAs bean<MyBean>()
  }

  @Test
  fun `should cache beans in beanMap`() {
    // when - first call
    val firstCall = bean<MyBean>()

    // then - bean is cached
    DiServiceContext.beanMap shouldContainKey MyBean::class.java
    DiServiceContext.beanMap[MyBean::class.java] shouldBeSameInstanceAs firstCall

    // when - second call
    val secondCall = bean<MyBean>()

    // then - same instance is returned from cache
    secondCall shouldBeSameInstanceAs firstCall
  }

  @Test
  fun `should cache different bean types separately`() {
    // when
    val myBean = bean<MyBean>()
    val anotherBean = bean<AnotherBean>()

    // then
    DiServiceContext.beanMap.size shouldBe 2
    DiServiceContext.beanMap[MyBean::class.java] shouldBeSameInstanceAs myBean
    DiServiceContext.beanMap[AnotherBean::class.java] shouldBeSameInstanceAs anotherBean
  }

  @Test
  fun `autowired should lazily initialize bean`() {
    // given - create a new lazy instance
    val lazyBean: Lazy<MyBean> = autowired()

    // when - force evaluation of the lazy
    val value1 = lazyBean.value
    val value2 = lazyBean.value

    // then - lazy should return the same bean instance
    value1 shouldNotBe null
    value2 shouldBeSameInstanceAs value1
    value1 shouldBeSameInstanceAs bean<MyBean>()
  }

  @Test
  fun `autowired should work with delegation`() {
    // given
    class BeanConsumer {
      val myBean: MyBean by autowired()
    }

    // when
    val consumer = BeanConsumer()

    // then
    consumer.myBean shouldNotBe null
    consumer.myBean shouldBeSameInstanceAs bean<MyBean>()
  }

  @Test
  fun `multiple autowired properties should work`() {
    // given
    class MultiBeanConsumer {
      val myBean: MyBean by autowired()
      val anotherBean: AnotherBean by autowired()
    }

    // when
    val consumer = MultiBeanConsumer()

    // then
    consumer.myBean shouldNotBe null
    consumer.anotherBean shouldNotBe null
    consumer.myBean shouldBeSameInstanceAs bean<MyBean>()
    consumer.anotherBean shouldBeSameInstanceAs bean<AnotherBean>()
  }

  @Test
  fun `should access application context`() {
    // when
    val ctx = DiServiceContext.ctx

    // then
    ctx shouldNotBe null
    ctx.getBean(MyBean::class.java) shouldNotBe null
  }

  @Test
  fun `beanMap should be mutable and modifiable`() {
    // given
    val customInstance = MyBean()

    // when
    DiServiceContext.beanMap[MyBean::class.java] = customInstance

    // then
    bean<MyBean>() shouldBeSameInstanceAs customInstance
  }
}
