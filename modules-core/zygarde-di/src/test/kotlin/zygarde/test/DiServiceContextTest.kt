package zygarde.test

import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import zygarde.core.di.DiServiceContext.autowired
import zygarde.core.di.DiServiceContext.bean

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = [DiServiceContextTest.DiServiceContextTestApp::class])
class DiServiceContextTest {

  class MyBean

  class MyBeanUser {
    val myBean: MyBean by autowired()
  }

  @SpringBootApplication
  class DiServiceContextTestApp {
    @Bean
    fun myBean() = MyBean()
    @Bean
    fun myBeanUser() = MyBeanUser()
  }

  @Test
  fun `bean and autowired`() {
    bean<MyBean>() shouldNotBe null
    bean<MyBeanUser>().myBean shouldBeSameInstanceAs bean<MyBean>()
  }
}
