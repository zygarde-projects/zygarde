package zygarde.api.openapi

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.RequestBody
import org.junit.jupiter.api.Test
import org.springframework.web.method.HandlerMethod
import zygarde.data.api.OpenApiSortableFields
import zygarde.data.api.PagingAndSortingRequest

class SortableFieldsOperationCustomizerTest {
  @OpenApiSortableFields("id", "title", "author.name", "id")
  class SearchBookReq : PagingAndSortingRequest()

  class TestController {
    fun search(req: SearchBookReq) = req
  }

  @Test
  fun `should overlay request schema without changing shared components`() {
    val originalSchema = Schema<Any>().apply { `$ref` = "#/components/schemas/SearchBookReq" }
    val operation = Operation().requestBody(
      RequestBody().content(
        Content().addMediaType("application/json", MediaType().schema(originalSchema))
      )
    )
    val method = TestController::class.java.getDeclaredMethod("search", SearchBookReq::class.java)
    val handlerMethod = HandlerMethod(TestController(), method)
    val customizer = SortableFieldsOperationCustomizer()

    customizer.customize(operation, handlerMethod)
    customizer.customize(operation, handlerMethod)

    val schema = operation.requestBody.content["application/json"]!!.schema as ComposedSchema
    schema.allOf.size shouldBe 2
    schema.allOf.first() shouldBe originalSchema
    val sorts = schema.allOf.last().properties["sorts"] as ArraySchema
    sorts.items.properties["field"]!!.enum shouldContainExactly listOf("id", "title", "author.name")
    sorts.items.properties["sort"]!!.enum shouldContainExactly listOf("ASC", "DESC")
  }
}
