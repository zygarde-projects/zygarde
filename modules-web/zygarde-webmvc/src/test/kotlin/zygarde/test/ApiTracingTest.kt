package zygarde.test

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.getForObject
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [ZygardeWebMvcTestApplication::class])
@ActiveProfiles("test")
class ApiTracingTest {

  @Autowired
  lateinit var testRestTemplate: TestRestTemplate

  @Test
  fun `get correct api id from tracing`() {
    testRestTemplate.getForObject<String>("/apiId") shouldBe "TestApi.getApiId"
  }
}
