package zygarde.codegen.generator

import com.squareup.kotlinpoet.asTypeName
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import zygarde.codegen.model.graphql.GraphQlApiToGenerateVo
import zygarde.codegen.model.graphql.GraphQlArgumentToGenerateVo
import zygarde.codegen.model.graphql.GraphQlFieldToGenerateVo
import zygarde.codegen.model.graphql.GraphQlFunctionToGenerateVo
import zygarde.codegen.model.graphql.GraphQlOperation
import zygarde.codegen.model.graphql.GraphQlTypeDefinitionKind
import zygarde.codegen.model.graphql.GraphQlTypeDefinitionToGenerateVo

data class GraphQlGeneratorTestTodoDto(val id: Int, val description: String)

data class GraphQlGeneratorTestTodoInput(val description: String)

data class GraphQlGeneratorTestTodoFilter(val descriptionContains: String?)

enum class GraphQlGeneratorTestTodoStatus {
  OPEN,
  DONE,
}

class GraphQlApiGeneratorTest {
  @Test
  fun `should generate controller service interface and schema`() {
    val result = GraphQlApiGenerator(
      listOf(
        GraphQlApiToGenerateVo(
          controllerPackage = "com.example.graphql",
          serviceInterfacePackage = "com.example.graphql.service",
          apiName = "TodoGraphQl",
          functions = mutableListOf(
            GraphQlFunctionToGenerateVo(
              operation = GraphQlOperation.QUERY,
              functionName = "todo",
              arguments = mutableListOf(
                GraphQlArgumentToGenerateVo(
                  name = "id",
                  type = Int::class.asTypeName(),
                  graphQlType = "Int",
                )
              ),
              responseType = GraphQlGeneratorTestTodoDto::class.asTypeName(),
              responseGraphQlType = "Todo",
              responseNullable = true,
              serviceName = "TodoGraphQlService",
            ),
            GraphQlFunctionToGenerateVo(
              operation = GraphQlOperation.QUERY,
              functionName = "todos",
              arguments = mutableListOf(
                GraphQlArgumentToGenerateVo(
                  name = "ids",
                  type = Int::class.asTypeName(),
                  graphQlType = "Int",
                  collection = true,
                ),
                GraphQlArgumentToGenerateVo(
                  name = "filter",
                  type = GraphQlGeneratorTestTodoFilter::class.asTypeName(),
                  graphQlType = "TodoFilter",
                  nullable = true,
                ),
                GraphQlArgumentToGenerateVo(
                  name = "status",
                  type = GraphQlGeneratorTestTodoStatus::class.asTypeName(),
                  graphQlType = "TodoStatus",
                  nullable = true,
                  defaultValue = "OPEN",
                )
              ),
              responseType = GraphQlGeneratorTestTodoDto::class.asTypeName(),
              responseGraphQlType = "Todo",
              responseCollection = true,
              serviceName = "TodoGraphQlService",
            ),
            GraphQlFunctionToGenerateVo(
              operation = GraphQlOperation.MUTATION,
              functionName = "createTodo",
              arguments = mutableListOf(
                GraphQlArgumentToGenerateVo(
                  name = "input",
                  type = GraphQlGeneratorTestTodoInput::class.asTypeName(),
                  graphQlType = "TodoInput",
                )
              ),
              responseType = GraphQlGeneratorTestTodoDto::class.asTypeName(),
              responseGraphQlType = "Todo",
              serviceName = "TodoGraphQlService",
            ),
            GraphQlFunctionToGenerateVo(
              operation = GraphQlOperation.SUBSCRIPTION,
              functionName = "todoEvents",
              responseType = GraphQlGeneratorTestTodoDto::class.asTypeName(),
              responseGraphQlType = "Todo",
              serviceName = "TodoGraphQlService",
            )
          ),
          typeDefinitions = mutableListOf(
            GraphQlTypeDefinitionToGenerateVo(
              kind = GraphQlTypeDefinitionKind.TYPE,
              name = "Todo",
              fields = mutableListOf(
                GraphQlFieldToGenerateVo("id", "Int"),
                GraphQlFieldToGenerateVo("description", "String"),
                GraphQlFieldToGenerateVo("status", "TodoStatus"),
                GraphQlFieldToGenerateVo("tags", "String", collection = true),
                GraphQlFieldToGenerateVo("previousDescriptions", "String", nullable = true, collection = true, itemNullable = true),
              )
            ),
            GraphQlTypeDefinitionToGenerateVo(
              kind = GraphQlTypeDefinitionKind.INPUT,
              name = "TodoInput",
              fields = mutableListOf(
                GraphQlFieldToGenerateVo("description", "String", defaultValue = "\"new todo\""),
                GraphQlFieldToGenerateVo("tags", "String", collection = true),
              )
            ),
            GraphQlTypeDefinitionToGenerateVo(
              kind = GraphQlTypeDefinitionKind.ENUM,
              name = "TodoStatus",
              enumValues = mutableListOf("OPEN", "DONE"),
            ),
            GraphQlTypeDefinitionToGenerateVo(
              kind = GraphQlTypeDefinitionKind.SCALAR,
              name = "Long",
            )
          )
        )
      )
    ).generateApis()

    result.controllers shouldHaveSize 1
    result.serviceInterfaces shouldHaveSize 1
    result.schemas shouldHaveSize 1

    val controller = result.controllers.single().toString()
    controller shouldContain "@Controller"
    controller shouldContain "@QueryMapping"
    controller shouldContain "@MutationMapping"
    controller shouldContain "@SubscriptionMapping"
    controller shouldContain "public fun todo(@Argument id: Int): GraphQlGeneratorTestTodoDto?"
    controller shouldContain "public fun todoEvents(): GraphQlGeneratorTestTodoDto"
    controller shouldContain "@Argument ids: Collection<Int>"
    controller shouldContain "filter: GraphQlGeneratorTestTodoFilter?"
    controller shouldContain "status: GraphQlGeneratorTestTodoStatus?"
    controller shouldContain "val service = bean<TodoGraphQlService>()"
    controller shouldContain "return service.todo(id)"
    controller shouldContain "return service.todos(ids, filter, status)"
    controller shouldContain "return service.createTodo(input)"
    controller shouldContain "return service.todoEvents()"

    val serviceInterface = result.serviceInterfaces.single().toString()
    serviceInterface shouldContain "public interface TodoGraphQlService"
    serviceInterface shouldContain "public fun todo(id: Int): GraphQlGeneratorTestTodoDto?"
    serviceInterface shouldContain "public fun todos("
    serviceInterface shouldContain "ids: Collection<Int>"
    serviceInterface shouldContain "filter: GraphQlGeneratorTestTodoFilter?"
    serviceInterface shouldContain "status: GraphQlGeneratorTestTodoStatus?"
    serviceInterface shouldContain "Collection<GraphQlGeneratorTestTodoDto>"
    serviceInterface shouldContain "public fun createTodo(input: GraphQlGeneratorTestTodoInput): GraphQlGeneratorTestTodoDto"
    serviceInterface shouldContain "public fun todoEvents(): GraphQlGeneratorTestTodoDto"

    val schema = result.schemas.single().content
    schema shouldContain "type Query"
    schema shouldContain "todo(id: Int!): Todo"
    schema shouldContain "todos(ids: [Int!]!, filter: TodoFilter, status: TodoStatus = OPEN): [Todo!]!"
    schema shouldContain "type Mutation"
    schema shouldContain "createTodo(input: TodoInput!): Todo!"
    schema shouldContain "type Subscription"
    schema shouldContain "todoEvents: Todo!"
    schema shouldContain "type Todo"
    schema shouldContain "status: TodoStatus!"
    schema shouldContain "tags: [String!]!"
    schema shouldContain "previousDescriptions: [String]"
    schema shouldContain "input TodoInput"
    schema shouldContain "description: String! = \"new todo\""
    schema shouldContain "enum TodoStatus"
    schema shouldContain "  OPEN"
    schema shouldContain "  DONE"
    schema shouldContain "scalar Long"
  }

  @Test
  fun `should extend operation root types after first generated schema`() {
    val result = GraphQlApiGenerator(
      listOf(
        GraphQlApiToGenerateVo(
          controllerPackage = "com.example.graphql",
          serviceInterfacePackage = "com.example.graphql.service",
          apiName = "TodoGraphQl",
          functions = mutableListOf(
            GraphQlFunctionToGenerateVo(
              operation = GraphQlOperation.QUERY,
              functionName = "todos",
              responseType = GraphQlGeneratorTestTodoDto::class.asTypeName(),
              responseGraphQlType = "Todo",
              responseCollection = true,
            ),
            GraphQlFunctionToGenerateVo(
              operation = GraphQlOperation.SUBSCRIPTION,
              functionName = "todoEvents",
              responseType = GraphQlGeneratorTestTodoDto::class.asTypeName(),
              responseGraphQlType = "Todo",
            )
          ),
        ),
        GraphQlApiToGenerateVo(
          controllerPackage = "com.example.graphql",
          serviceInterfacePackage = "com.example.graphql.service",
          apiName = "ArchiveGraphQl",
          functions = mutableListOf(
            GraphQlFunctionToGenerateVo(
              operation = GraphQlOperation.QUERY,
              functionName = "archivedTodos",
              responseType = GraphQlGeneratorTestTodoDto::class.asTypeName(),
              responseGraphQlType = "Todo",
              responseCollection = true,
            ),
            GraphQlFunctionToGenerateVo(
              operation = GraphQlOperation.MUTATION,
              functionName = "restoreTodo",
              arguments = mutableListOf(
                GraphQlArgumentToGenerateVo(
                  name = "id",
                  type = Int::class.asTypeName(),
                  graphQlType = "Int",
                )
              ),
              responseType = GraphQlGeneratorTestTodoDto::class.asTypeName(),
              responseGraphQlType = "Todo",
            )
          ),
        ),
        GraphQlApiToGenerateVo(
          controllerPackage = "com.example.graphql",
          serviceInterfacePackage = "com.example.graphql.service",
          apiName = "AdminGraphQl",
          functions = mutableListOf(
            GraphQlFunctionToGenerateVo(
              operation = GraphQlOperation.MUTATION,
              functionName = "deleteArchivedTodo",
              arguments = mutableListOf(
                GraphQlArgumentToGenerateVo(
                  name = "id",
                  type = Int::class.asTypeName(),
                  graphQlType = "Int",
                )
              ),
              responseType = Boolean::class.asTypeName(),
              responseGraphQlType = "Boolean",
            ),
            GraphQlFunctionToGenerateVo(
              operation = GraphQlOperation.SUBSCRIPTION,
              functionName = "adminEvents",
              responseType = GraphQlGeneratorTestTodoDto::class.asTypeName(),
              responseGraphQlType = "Todo",
            )
          ),
        )
      )
    ).generateApis()

    result.schemas shouldHaveSize 3
    result.schemas[0].content shouldContain "type Query"
    result.schemas[0].content shouldContain "type Subscription"
    result.schemas[1].content shouldContain "extend type Query"
    result.schemas[1].content shouldContain "type Mutation"
    result.schemas[2].content shouldContain "extend type Mutation"
    result.schemas[2].content shouldContain "extend type Subscription"
  }

  @Test
  fun `should generate nullable collection response and nullable collection items`() {
    val result = GraphQlApiGenerator(
      listOf(
        GraphQlApiToGenerateVo(
          controllerPackage = "com.example.graphql",
          serviceInterfacePackage = "com.example.graphql.service",
          apiName = "TodoGraphQl",
          functions = mutableListOf(
            GraphQlFunctionToGenerateVo(
              operation = GraphQlOperation.QUERY,
              functionName = "nullableTodos",
              responseType = GraphQlGeneratorTestTodoDto::class.asTypeName(),
              responseGraphQlType = "Todo",
              responseCollection = true,
              responseNullable = true,
              responseItemNullable = true,
            )
          ),
        )
      )
    ).generateApis()

    result.controllers.single().toString() shouldContain "public fun nullableTodos(): Collection<GraphQlGeneratorTestTodoDto?>?"
    result.serviceInterfaces.single().toString() shouldContain "public fun nullableTodos(): Collection<GraphQlGeneratorTestTodoDto?>?"
    result.schemas.single().content shouldContain "nullableTodos: [Todo]"
  }

  @Test
  fun `should generate nullable collection argument and nullable collection argument items`() {
    val result = GraphQlApiGenerator(
      listOf(
        GraphQlApiToGenerateVo(
          controllerPackage = "com.example.graphql",
          serviceInterfacePackage = "com.example.graphql.service",
          apiName = "TodoGraphQl",
          functions = mutableListOf(
            GraphQlFunctionToGenerateVo(
              operation = GraphQlOperation.QUERY,
              functionName = "todosByOptionalIds",
              arguments = mutableListOf(
                GraphQlArgumentToGenerateVo(
                  name = "ids",
                  type = Int::class.asTypeName(),
                  graphQlType = "Int",
                  nullable = true,
                  collection = true,
                  itemNullable = true,
                )
              ),
              responseType = GraphQlGeneratorTestTodoDto::class.asTypeName(),
              responseGraphQlType = "Todo",
              responseCollection = true,
            )
          ),
        )
      )
    ).generateApis()

    val controller = result.controllers.single().toString()
    controller shouldContain "public fun todosByOptionalIds(@Argument ids: Collection<Int?>?):"
    controller shouldContain "Collection<GraphQlGeneratorTestTodoDto>"

    val serviceInterface = result.serviceInterfaces.single().toString()
    serviceInterface shouldContain "public fun todosByOptionalIds(ids: Collection<Int?>?):"
    serviceInterface shouldContain "Collection<GraphQlGeneratorTestTodoDto>"
    result.schemas.single().content shouldContain "todosByOptionalIds(ids: [Int]): [Todo!]!"
  }

  @Test
  fun `should reject invalid GraphQL names before rendering generated output`() {
    shouldThrow<IllegalArgumentException> {
      GraphQlApiGenerator(
        listOf(
          GraphQlApiToGenerateVo(
            controllerPackage = "com.example.graphql",
            serviceInterfacePackage = "com.example.graphql.service",
            apiName = "TodoGraphQl",
            functions = mutableListOf(
              GraphQlFunctionToGenerateVo(
                operation = GraphQlOperation.QUERY,
                functionName = "todo-list",
                responseType = GraphQlGeneratorTestTodoDto::class.asTypeName(),
                responseGraphQlType = "Todo",
              )
            ),
          )
        )
      ).generateApis()
    }.message shouldBe "GraphQL query field must be a valid GraphQL name"
  }
}
