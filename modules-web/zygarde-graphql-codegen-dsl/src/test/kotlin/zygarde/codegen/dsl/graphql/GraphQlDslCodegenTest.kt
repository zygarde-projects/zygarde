package zygarde.codegen.dsl.graphql

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow
import org.junit.jupiter.api.Test
import zygarde.codegen.model.graphql.GraphQlOperation
import zygarde.codegen.model.graphql.GraphQlTypeDefinitionKind

class GraphQlDslCodegenTest {
  data class TodoDto(val id: Int, val description: String)

  data class TodoInput(val description: String)

  data class TodoFilter(val descriptionContains: String?)

  enum class TodoStatus {
    OPEN,
    DONE,
  }

  @Test
  fun `should convert query mutation subscription nullable argument collection response and scalar response`() {
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
            argument<TodoFilter>("filter", "TodoFilter", nullable = true, defaultValue = "{ descriptionContains: \"open\" }")
            argument<TodoStatus>("status", nullable = true)
            returnsCollection<TodoDto>("Todo", nullable = true, itemNullable = true)
            serviceName = "TodoGraphQlService"
          }
          mutation("deleteTodo") {
            argument<Int>("id")
            returns<Boolean>()
            serviceName = "TodoGraphQlService"
          }
          subscription("todoEvents") {
            argument<Int>("id", nullable = true)
            returns<TodoDto>("Todo")
            serviceName = "TodoGraphQlService"
          }
          type("Todo") {
            field<Int>("id")
            field<String>("description")
            collectionField<String>("tags")
            collectionField<String>("previousDescriptions", nullable = true, itemNullable = true)
          }
          input("TodoFilter") {
            field<String>("descriptionContains", nullable = true, defaultValue = "\"open\"")
            collectionField<Int>("ids", defaultValue = "[]")
          }
          enumType("TodoStatus") {
            value("OPEN")
            value("DONE")
          }
        }
      }
    }

    dsl.codegen()

    val api = dsl.apisToGenerate.single()
    api.apiName shouldBe "TodoGraphQl"
    api.functions shouldHaveSize 4
    api.typeDefinitions shouldHaveSize 3

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
      arguments[1].defaultValue shouldBe "{ descriptionContains: \"open\" }"
      arguments[2].apply {
        graphQlType shouldBe "TodoStatus"
        nullable shouldBe true
      }
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

    api.functions[3].apply {
      operation shouldBe GraphQlOperation.SUBSCRIPTION
      functionName shouldBe "todoEvents"
      arguments.single().apply {
        graphQlType shouldBe "Int"
        nullable shouldBe true
      }
      responseCollection shouldBe false
      responseNullable shouldBe false
      responseGraphQlType shouldBe "Todo"
      serviceName shouldBe "TodoGraphQlService"
    }

    api.typeDefinitions[0].kind shouldBe GraphQlTypeDefinitionKind.TYPE
    api.typeDefinitions[0].fields[2].apply {
      graphQlType shouldBe "String"
      nullable shouldBe false
      collection shouldBe true
      itemNullable shouldBe false
    }
    api.typeDefinitions[0].fields[3].apply {
      graphQlType shouldBe "String"
      nullable shouldBe true
      collection shouldBe true
      itemNullable shouldBe true
    }
    api.typeDefinitions[1].kind shouldBe GraphQlTypeDefinitionKind.INPUT
    api.typeDefinitions[1].fields[1].apply {
      graphQlType shouldBe "Int"
      nullable shouldBe false
      collection shouldBe true
      itemNullable shouldBe false
      defaultValue shouldBe "[]"
    }
    api.typeDefinitions[2].kind shouldBe GraphQlTypeDefinitionKind.ENUM
    api.typeDefinitions[2].enumValues shouldBe mutableListOf("OPEN", "DONE")
  }

  @Test
  fun `should reject default values on object type fields`() {
    shouldThrow<IllegalArgumentException> {
      DslGraphQlTypeDefinition.type("Todo")
        .field<String>("description", defaultValue = "\"open\"")
    }.message shouldBe "GraphQL field default values are only supported on input fields"
  }
}
