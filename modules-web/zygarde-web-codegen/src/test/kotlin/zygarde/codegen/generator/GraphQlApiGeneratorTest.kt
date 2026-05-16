package zygarde.codegen.generator

import com.squareup.kotlinpoet.asTypeName
import io.kotest.matchers.collections.shouldHaveSize
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
                  name = "filter",
                  type = GraphQlGeneratorTestTodoFilter::class.asTypeName(),
                  graphQlType = "TodoFilter",
                  nullable = true,
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
            )
          ),
          typeDefinitions = mutableListOf(
            GraphQlTypeDefinitionToGenerateVo(
              kind = GraphQlTypeDefinitionKind.TYPE,
              name = "Todo",
              fields = mutableListOf(
                GraphQlFieldToGenerateVo("id", "Int"),
                GraphQlFieldToGenerateVo("description", "String"),
              )
            ),
            GraphQlTypeDefinitionToGenerateVo(
              kind = GraphQlTypeDefinitionKind.INPUT,
              name = "TodoInput",
              fields = mutableListOf(
                GraphQlFieldToGenerateVo("description", "String"),
              )
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
    controller shouldContain "public fun todo(@Argument id: Int): GraphQlGeneratorTestTodoDto?"
    controller shouldContain "@Argument filter: GraphQlGeneratorTestTodoFilter?"
    controller shouldContain "val service = bean<TodoGraphQlService>()"
    controller shouldContain "return service.todo(id)"
    controller shouldContain "return service.todos(filter)"
    controller shouldContain "return service.createTodo(input)"

    val serviceInterface = result.serviceInterfaces.single().toString()
    serviceInterface shouldContain "public interface TodoGraphQlService"
    serviceInterface shouldContain "public fun todo(id: Int): GraphQlGeneratorTestTodoDto?"
    serviceInterface shouldContain "public fun todos(filter: GraphQlGeneratorTestTodoFilter?): Collection<GraphQlGeneratorTestTodoDto>"
    serviceInterface shouldContain "public fun createTodo(input: GraphQlGeneratorTestTodoInput): GraphQlGeneratorTestTodoDto"

    val schema = result.schemas.single().content
    schema shouldContain "type Query"
    schema shouldContain "todo(id: Int!): Todo"
    schema shouldContain "todos(filter: TodoFilter): [Todo!]!"
    schema shouldContain "type Mutation"
    schema shouldContain "createTodo(input: TodoInput!): Todo!"
    schema shouldContain "type Todo"
    schema shouldContain "input TodoInput"
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
            )
          ),
        )
      )
    ).generateApis()

    result.schemas shouldHaveSize 3
    result.schemas[0].content shouldContain "type Query"
    result.schemas[1].content shouldContain "extend type Query"
    result.schemas[1].content shouldContain "type Mutation"
    result.schemas[2].content shouldContain "extend type Mutation"
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
}
