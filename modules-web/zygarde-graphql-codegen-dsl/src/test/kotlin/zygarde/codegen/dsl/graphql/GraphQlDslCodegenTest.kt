package zygarde.codegen.dsl.graphql

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import zygarde.codegen.model.graphql.GraphQlOperation
import zygarde.codegen.model.graphql.GraphQlTypeDefinitionKind

class GraphQlDslCodegenTest {
  data class TodoDto(val id: Int, val description: String)

  data class TodoInput(val description: String)

  data class TodoFilter(val descriptionContains: String?)

  @Test
  fun `should convert query mutation nullable argument collection response and scalar response`() {
    val dsl = object : GraphQlDslCodegen() {
      override fun codegen() {
        schema("TodoGraphQl") {
          query("todo") {
            argument<Int>("id")
            returns<TodoDto>("Todo", nullable = true)
            serviceName = "TodoGraphQlService"
          }
          query("todos") {
            collectionArgument<Int>("ids")
            argument<TodoFilter>("filter", "TodoFilter", nullable = true)
            returnsCollection<TodoDto>("Todo", nullable = true, itemNullable = true)
            serviceName = "TodoGraphQlService"
          }
          mutation("deleteTodo") {
            argument<Int>("id")
            returns<Boolean>()
            serviceName = "TodoGraphQlService"
          }
          type("Todo") {
            field<Int>("id")
            field<String>("description")
          }
          input("TodoFilter") {
            field<String>("descriptionContains", nullable = true)
          }
        }
      }
    }

    dsl.codegen()

    val api = dsl.apisToGenerate.single()
    api.apiName shouldBe "TodoGraphQl"
    api.functions shouldHaveSize 3
    api.typeDefinitions shouldHaveSize 2

    api.functions[0].apply {
      operation shouldBe GraphQlOperation.QUERY
      functionName shouldBe "todo"
      arguments.single().graphQlType shouldBe "Int"
      responseCollection shouldBe false
      responseNullable shouldBe true
      responseGraphQlType shouldBe "Todo"
      serviceName shouldBe "TodoGraphQlService"
    }

    api.functions[1].apply {
      operation shouldBe GraphQlOperation.QUERY
      functionName shouldBe "todos"
      arguments[0].apply {
        graphQlType shouldBe "Int"
        nullable shouldBe false
        collection shouldBe true
        itemNullable shouldBe false
      }
      arguments[1].nullable shouldBe true
      responseCollection shouldBe true
      responseNullable shouldBe true
      responseItemNullable shouldBe true
      responseGraphQlType shouldBe "Todo"
      serviceName shouldBe "TodoGraphQlService"
    }

    api.functions[2].apply {
      operation shouldBe GraphQlOperation.MUTATION
      functionName shouldBe "deleteTodo"
      arguments.single().graphQlType shouldBe "Int"
      responseCollection shouldBe false
      responseNullable shouldBe false
      responseGraphQlType shouldBe "Boolean"
    }

    api.typeDefinitions[0].kind shouldBe GraphQlTypeDefinitionKind.TYPE
    api.typeDefinitions[1].kind shouldBe GraphQlTypeDefinitionKind.INPUT
  }
}
