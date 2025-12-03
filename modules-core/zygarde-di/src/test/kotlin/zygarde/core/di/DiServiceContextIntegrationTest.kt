package zygarde.core.di

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean

@SpringBootTest(classes = [DiServiceContextIntegrationTest.TestApp::class])
class DiServiceContextIntegrationTest {
  @Autowired
  lateinit var applicationContext: ApplicationContext

  class ServiceA

  class ServiceB(val serviceA: ServiceA)

  @SpringBootApplication
  class TestApp {
    @Bean
    fun serviceA() = ServiceA()

    @Bean
    fun serviceB(serviceA: ServiceA) = ServiceB(serviceA)
  }

  @Test
  fun `DiServiceContext should be registered as ApplicationContextAware`() {
    // then - DiServiceContext ctx should be set by Spring
    DiServiceContext.ctx shouldBeSameInstanceAs applicationContext
  }

  @Test
  fun `bean function should retrieve beans from Spring context`() {
    // when
    val serviceA = DiServiceContext.bean<ServiceA>()
    val serviceB = DiServiceContext.bean<ServiceB>()

    // then
    serviceA shouldNotBe null
    serviceB shouldNotBe null
    serviceB.serviceA shouldBeSameInstanceAs serviceA
  }

  @Test
  fun `bean function should cache retrieved beans`() {
    // given - clear cache
    DiServiceContext.beanMap.clear()

    // when - retrieve bean twice
    val first = DiServiceContext.bean<ServiceA>()
    val second = DiServiceContext.bean<ServiceA>()

    // then - same instance from cache
    first shouldBeSameInstanceAs second
    DiServiceContext.beanMap.size shouldBe 1
  }

  @Test
  fun `autowired should create lazy delegate`() {
    // given
    val lazyService by DiServiceContext.autowired<ServiceA>()

    // when
    val service = lazyService

    // then
    service shouldNotBe null
    service shouldBeSameInstanceAs DiServiceContext.bean<ServiceA>()
  }
}
