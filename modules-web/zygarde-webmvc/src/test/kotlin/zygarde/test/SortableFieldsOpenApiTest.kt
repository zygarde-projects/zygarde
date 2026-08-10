package zygarde.test

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.getForObject
import org.springframework.test.context.ActiveProfiles
import zygarde.test.api.SearchBookReq

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [ZygardeWebMvcTestApplication::class])
@ActiveProfiles("test")
class SortableFieldsOpenApiTest {
  @Autowired
  lateinit var testRestTemplate: TestRestTemplate

  @Autowired
  lateinit var objectMapper: ObjectMapper

  @Test
  fun `should expose request-local sortable field enum in OpenAPI`() {
    val openApi = objectMapper.readTree(testRestTemplate.getForObject<String>("/v3/api-docs"))
    val requestSchema = openApi.at(
      "/paths/~1sortable-books~1search/post/requestBody/content/application~1json/schema"
    )
    val fieldEnum = requestSchema.at("/allOf/1/properties/sorts/items/properties/field/enum")

    fieldEnum.map { it.textValue() } shouldContainExactly listOf("id", "title", "author.name")
    openApi.at("/components/schemas/SortField/properties/field/enum").isMissingNode shouldBe true
  }

  @Test
  fun `should keep sortable field runtime value as unrestricted string`() {
    val req = objectMapper.readValue<SearchBookReq>(
      """{"sorts":[{"sort":"DESC","field":"not-in-openapi-enum"}]}"""
    )

    req.sorts!!.single().field shouldBe "not-in-openapi-enum"
  }
}
