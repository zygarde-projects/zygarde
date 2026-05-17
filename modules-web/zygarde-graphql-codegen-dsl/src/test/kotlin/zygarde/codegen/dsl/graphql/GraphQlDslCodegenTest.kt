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
            argument<TodoFilter>(
              "filter",
              "TodoFilter",
              nullable = true,
              defaultValue = GraphQlDefaultValue.objectValue("descriptionContains" to GraphQlDefaultValue.string("open"))
            )
            argument<TodoStatus>("status", nullable = true, defaultValue = GraphQlDefaultValue.enum(TodoStatus.OPEN))
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
            field<String>("descriptionContains", nullable = true, defaultValue = GraphQlDefaultValue.string("open"))
            collectionField<Int>("ids", defaultValue = GraphQlDefaultValue.list())
          }
          enumType<TodoStatus>()
          scalar<Long>()
        }
      }
    }

    dsl.codegen()

    val api = dsl.apisToGenerate.single()
    api.apiName shouldBe "TodoGraphQl"
    api.functions shouldHaveSize 4
    api.typeDefinitions shouldHaveSize 4

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
        defaultValue shouldBe "OPEN"
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
    api.typeDefinitions[3].kind shouldBe GraphQlTypeDefinitionKind.SCALAR
    api.typeDefinitions[3].name shouldBe "Long"
  }

  @Test
  fun `should reject default values on object type fields`() {
    shouldThrow<IllegalArgumentException> {
      DslGraphQlTypeDefinition.type("Todo")
        .field<String>("description", defaultValue = "\"open\"")
    }.message shouldBe "GraphQL field default values are only supported on input fields"
  }

  @Test
  fun `should build GraphQL default value literals`() {
    GraphQlDefaultValue.string("open \"todo\"\\\nnext") shouldBe "\"open \\\"todo\\\"\\\\\\nnext\""
    GraphQlDefaultValue.int(1) shouldBe "1"
    GraphQlDefaultValue.long(2L) shouldBe "2"
    GraphQlDefaultValue.float(3.5f) shouldBe "3.5"
    GraphQlDefaultValue.double(4.25) shouldBe "4.25"
    GraphQlDefaultValue.boolean(true) shouldBe "true"
    GraphQlDefaultValue.enum(TodoStatus.DONE) shouldBe "DONE"
    GraphQlDefaultValue.list(GraphQlDefaultValue.int(1), GraphQlDefaultValue.nullValue()) shouldBe "[1, null]"
    GraphQlDefaultValue.objectValue(
      "descriptionContains" to GraphQlDefaultValue.string("open"),
      "status" to GraphQlDefaultValue.enum(TodoStatus.OPEN),
    ) shouldBe "{ descriptionContains: \"open\", status: OPEN }"
  }

  @Test
  fun `should reject invalid GraphQL default value names`() {
    shouldThrow<IllegalArgumentException> {
      GraphQlDefaultValue.enum("not-valid")
    }.message shouldBe "GraphQL enum default value must be a valid GraphQL name"

    shouldThrow<IllegalArgumentException> {
      GraphQlDefaultValue.objectValue("not-valid" to GraphQlDefaultValue.string("open"))
    }.message shouldBe "GraphQL object default field must be a valid GraphQL name"
  }

  @Test
  fun `should reject invalid GraphQL declaration names`() {
    shouldThrow<IllegalArgumentException> {
      object : GraphQlDslCodegen() {
        override fun codegen() {
          schema("TodoGraphQl") {
            query("todo-list") {
              returns<TodoDto>("Todo")
            }
          }
        }
      }.codegen()
    }.message shouldBe "GraphQL query field must be a valid GraphQL name"

    shouldThrow<IllegalArgumentException> {
      DslGraphQlTypeDefinition.input("TodoInput")
        .field<String>("not-valid")
    }.message shouldBe "GraphQL field name must be a valid GraphQL name"
  }

  @Test
  fun `should reject reserved GraphQL introspection names`() {
    shouldThrow<IllegalArgumentException> {
      object : GraphQlDslCodegen() {
        override fun codegen() {
          schema("TodoGraphQl") {
            query("__todos") {
              returns<TodoDto>("Todo")
            }
          }
        }
      }.codegen()
    }.message shouldBe "GraphQL query field must not start with '__' because GraphQL reserves introspection names"

    shouldThrow<IllegalArgumentException> {
      DslGraphQlTypeDefinition.type("__Todo")
    }.message shouldBe "GraphQL type definition name must not start with '__' because GraphQL reserves introspection names"

    shouldThrow<IllegalArgumentException> {
      GraphQlDefaultValue.enum("__OPEN")
    }.message shouldBe "GraphQL enum default value must not start with '__' because GraphQL reserves introspection names"
  }

  @Test
  fun `should reject duplicate GraphQL declarations`() {
    shouldThrow<IllegalArgumentException> {
      object : GraphQlDslCodegen() {
        override fun codegen() {
          schema("TodoGraphQl") {
            query("todos") {
              returnsCollection<TodoDto>("Todo")
            }
            query("todos") {
              returnsCollection<TodoDto>("Todo")
            }
          }
        }
      }.codegen()
    }.message shouldBe "GraphQL query field 'todos' is already declared"

    shouldThrow<IllegalArgumentException> {
      object : GraphQlDslCodegen() {
        override fun codegen() {
          schema("TodoGraphQl") {
            type("Todo") {
              field<Int>("id")
            }
            input("Todo") {
              field<Int>("id")
            }
          }
        }
      }.codegen()
    }.message shouldBe "GraphQL type definition 'Todo' is already declared"

    shouldThrow<IllegalArgumentException> {
      DslGraphQlFunction("todo", GraphQlOperation.QUERY).apply {
        argument<Int>("id")
        argument<Int>("id")
      }
    }.message shouldBe "GraphQL argument 'id' is already declared"

    shouldThrow<IllegalArgumentException> {
      DslGraphQlTypeDefinition.type("Todo").apply {
        field<Int>("id")
        field<Int>("id")
      }
    }.message shouldBe "GraphQL field 'id' is already declared"

    shouldThrow<IllegalArgumentException> {
      DslGraphQlTypeDefinition.enumType("TodoStatus").apply {
        value("OPEN")
        value("OPEN")
      }
    }.message shouldBe "GraphQL enum value 'OPEN' is already declared"
  }
}
