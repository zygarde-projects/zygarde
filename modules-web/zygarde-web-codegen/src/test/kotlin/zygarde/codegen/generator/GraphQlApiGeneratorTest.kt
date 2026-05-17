package zygarde.codegen.generator

import com.squareup.kotlinpoet.asTypeName
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import org.junit.jupiter.api.Test
import zygarde.codegen.model.graphql.GraphQlApiToGenerateVo
import zygarde.codegen.model.graphql.GraphQlArgumentToGenerateVo
import zygarde.codegen.model.graphql.GraphQlEnumValueToGenerateVo
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
              enumValues = mutableListOf(
                GraphQlEnumValueToGenerateVo("OPEN"),
                GraphQlEnumValueToGenerateVo("DONE"),
              ),
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
    controller shouldContain "@QueryMapping(name = \"todo\")"
    controller shouldContain "@QueryMapping(name = \"todos\")"
    controller shouldContain "@MutationMapping(name = \"createTodo\")"
    controller shouldContain "@SubscriptionMapping(name = \"todoEvents\")"
    controller shouldContain "public fun todo(@Argument(name = \"id\") id: Int): GraphQlGeneratorTestTodoDto?"
    controller shouldContain "public fun todoEvents(): GraphQlGeneratorTestTodoDto"
    controller shouldContain "@Argument(name = \"ids\") ids: Collection<Int>"
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
  fun `should generate distinct Kotlin methods for GraphQL field names shared by operation roots`() {
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
              serviceName = "TodoGraphQlService",
            ),
            GraphQlFunctionToGenerateVo(
              operation = GraphQlOperation.MUTATION,
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
              serviceName = "TodoGraphQlService",
            )
          ),
        )
      )
    ).generateApis()

    val controller = result.controllers.single().toString()
    controller shouldContain "@QueryMapping(name = \"todo\")"
    controller shouldContain "public fun queryTodo(@Argument(name = \"id\") id: Int): GraphQlGeneratorTestTodoDto"
    controller shouldContain "@MutationMapping(name = \"todo\")"
    controller shouldContain "public fun mutationTodo(@Argument(name = \"id\") id: Int): GraphQlGeneratorTestTodoDto"
    controller shouldContain "return service.queryTodo(id)"
    controller shouldContain "return service.mutationTodo(id)"

    val serviceInterface = result.serviceInterfaces.single().toString()
    serviceInterface shouldContain "public fun queryTodo(id: Int): GraphQlGeneratorTestTodoDto"
    serviceInterface shouldContain "public fun mutationTodo(id: Int): GraphQlGeneratorTestTodoDto"

    val schema = result.schemas.single().content
    schema shouldContain "type Query"
    schema shouldContain "todo(id: Int!): Todo!"
    schema shouldContain "type Mutation"
    schema shouldContain "todo(id: Int!): Todo!"
  }

  @Test
  fun `should escape Kotlin keywords when calling generated GraphQL service methods`() {
    val result = GraphQlApiGenerator(
      listOf(
        GraphQlApiToGenerateVo(
          controllerPackage = "com.example.graphql",
          serviceInterfacePackage = "com.example.graphql.service",
          apiName = "TodoGraphQl",
          functions = mutableListOf(
            GraphQlFunctionToGenerateVo(
              operation = GraphQlOperation.QUERY,
              functionName = "class",
              arguments = mutableListOf(
                GraphQlArgumentToGenerateVo(
                  name = "in",
                  type = Int::class.asTypeName(),
                  graphQlType = "Int",
                )
              ),
              responseType = GraphQlGeneratorTestTodoDto::class.asTypeName(),
              responseGraphQlType = "Todo",
              serviceName = "TodoGraphQlService",
            )
          ),
        )
      )
    ).generateApis()

    val controller = result.controllers.single().toString()
    controller shouldContain "@QueryMapping(name = \"class\")"
    controller shouldContain "public fun `class`(@Argument(name = \"in\") `in`: Int): GraphQlGeneratorTestTodoDto"
    controller shouldContain "return service.`class`(`in`)"

    val serviceInterface = result.serviceInterfaces.single().toString()
    serviceInterface shouldContain "public fun `class`(`in`: Int): GraphQlGeneratorTestTodoDto"

    val schema = result.schemas.single().content
    schema shouldContain "type Query"
    schema shouldContain "class(in: Int!): Todo!"
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
    controller shouldContain "public fun todosByOptionalIds(@Argument(name = \"ids\") ids: Collection<Int?>?):"
    controller shouldContain "Collection<GraphQlGeneratorTestTodoDto>"

    val serviceInterface = result.serviceInterfaces.single().toString()
    serviceInterface shouldContain "public fun todosByOptionalIds(ids: Collection<Int?>?):"
    serviceInterface shouldContain "Collection<GraphQlGeneratorTestTodoDto>"
    result.schemas.single().content shouldContain "todosByOptionalIds(ids: [Int]): [Todo!]!"
  }

  @Test
  fun `should generate schema-only SDL without empty controller or service interface`() {
    val result = GraphQlApiGenerator(
      listOf(
        GraphQlApiToGenerateVo(
          controllerPackage = "com.example.graphql",
          serviceInterfacePackage = "com.example.graphql.service",
          apiName = "TodoGraphQl",
          typeDefinitions = mutableListOf(
            GraphQlTypeDefinitionToGenerateVo(
              kind = GraphQlTypeDefinitionKind.SCALAR,
              name = "DateTime",
            ),
            GraphQlTypeDefinitionToGenerateVo(
              kind = GraphQlTypeDefinitionKind.TYPE,
              name = "Todo",
              fields = mutableListOf(
                GraphQlFieldToGenerateVo("id", "Int"),
                GraphQlFieldToGenerateVo("description", "String"),
              )
            )
          ),
        )
      )
    ).generateApis()

    result.controllers shouldHaveSize 0
    result.serviceInterfaces shouldHaveSize 0
    result.schemas shouldHaveSize 1
    result.schemas.single().content shouldStartWith "scalar DateTime\n\n"
    result.schemas.single().content shouldContain "type Todo"
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

  @Test
  fun `should reject reserved GraphQL introspection names before rendering generated output`() {
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
                functionName = "__todos",
                responseType = GraphQlGeneratorTestTodoDto::class.asTypeName(),
                responseGraphQlType = "Todo",
              )
            ),
          )
        )
      ).generateApis()
    }.message shouldBe "GraphQL query field must not start with '__' because GraphQL reserves introspection names"
  }

  @Test
  fun `should reject duplicate GraphQL operation fields before rendering generated output`() {
    shouldThrow<IllegalArgumentException> {
      GraphQlApiGenerator(
        listOf(
          graphQlApi(
            apiName = "TodoGraphQl",
            functions = mutableListOf(todoQuery("todos"))
          ),
          graphQlApi(
            apiName = "ArchiveGraphQl",
            functions = mutableListOf(todoQuery("todos"))
          )
        )
      ).generateApis()
    }.message shouldBe "GraphQL query field 'todos' is already declared"
  }

  @Test
  fun `should reject duplicate GraphQL type definitions before rendering generated output`() {
    shouldThrow<IllegalArgumentException> {
      GraphQlApiGenerator(
        listOf(
          graphQlApi(
            apiName = "TodoGraphQl",
            typeDefinitions = mutableListOf(
              GraphQlTypeDefinitionToGenerateVo(
                kind = GraphQlTypeDefinitionKind.TYPE,
                name = "Todo",
                fields = mutableListOf(GraphQlFieldToGenerateVo("id", "Int")),
              )
            )
          ),
          graphQlApi(
            apiName = "ArchiveGraphQl",
            typeDefinitions = mutableListOf(
              GraphQlTypeDefinitionToGenerateVo(
                kind = GraphQlTypeDefinitionKind.INPUT,
                name = "Todo",
                fields = mutableListOf(GraphQlFieldToGenerateVo("id", "Int")),
              )
            )
          )
        )
      ).generateApis()
    }.message shouldBe "GraphQL type definition 'Todo' is already declared"
  }

  @Test
  fun `should reject duplicate GraphQL APIs before rendering generated output`() {
    shouldThrow<IllegalArgumentException> {
      GraphQlApiGenerator(
        listOf(
          graphQlApi(apiName = "TodoGraphQl"),
          graphQlApi(apiName = "TodoGraphQl")
        )
      ).generateApis()
    }.message shouldBe "GraphQL API 'TodoGraphQl' is already declared"
  }

  @Test
  fun `should reject empty GraphQL type definitions before rendering generated output`() {
    shouldThrow<IllegalArgumentException> {
      GraphQlApiGenerator(
        listOf(
          graphQlApi(
            typeDefinitions = mutableListOf(
              GraphQlTypeDefinitionToGenerateVo(GraphQlTypeDefinitionKind.TYPE, "Todo")
            )
          )
        )
      ).generateApis()
    }.message shouldBe "GraphQL type 'Todo' must declare at least one field"

    shouldThrow<IllegalArgumentException> {
      GraphQlApiGenerator(
        listOf(
          graphQlApi(
            typeDefinitions = mutableListOf(
              GraphQlTypeDefinitionToGenerateVo(GraphQlTypeDefinitionKind.INPUT, "TodoInput")
            )
          )
        )
      ).generateApis()
    }.message shouldBe "GraphQL input 'TodoInput' must declare at least one field"

    shouldThrow<IllegalArgumentException> {
      GraphQlApiGenerator(
        listOf(
          graphQlApi(
            typeDefinitions = mutableListOf(
              GraphQlTypeDefinitionToGenerateVo(GraphQlTypeDefinitionKind.ENUM, "TodoStatus")
            )
          )
        )
      ).generateApis()
    }.message shouldBe "GraphQL enum 'TodoStatus' must declare at least one value"
  }

  @Test
  fun `should reject default values on generated GraphQL object type fields`() {
    shouldThrow<IllegalArgumentException> {
      GraphQlApiGenerator(
        listOf(
          graphQlApi(
            typeDefinitions = mutableListOf(
              GraphQlTypeDefinitionToGenerateVo(
                kind = GraphQlTypeDefinitionKind.TYPE,
                name = "Todo",
                fields = mutableListOf(
                  GraphQlFieldToGenerateVo("description", "String", defaultValue = "\"open\""),
                )
              )
            )
          )
        )
      ).generateApis()
    }.message shouldBe "GraphQL field default values are only supported on input fields"
  }

  @Test
  fun `should reject duplicate GraphQL nested declarations before rendering generated output`() {
    shouldThrow<IllegalArgumentException> {
      GraphQlApiGenerator(
        listOf(
          graphQlApi(
            functions = mutableListOf(
              todoQuery(
                arguments = mutableListOf(
                  GraphQlArgumentToGenerateVo("id", Int::class.asTypeName(), "Int"),
                  GraphQlArgumentToGenerateVo("id", Int::class.asTypeName(), "Int"),
                )
              )
            )
          )
        )
      ).generateApis()
    }.message shouldBe "GraphQL query field 'todo' argument 'id' is already declared"

    shouldThrow<IllegalArgumentException> {
      GraphQlApiGenerator(
        listOf(
          graphQlApi(
            typeDefinitions = mutableListOf(
              GraphQlTypeDefinitionToGenerateVo(
                kind = GraphQlTypeDefinitionKind.TYPE,
                name = "Todo",
                fields = mutableListOf(
                  GraphQlFieldToGenerateVo("id", "Int"),
                  GraphQlFieldToGenerateVo("id", "Int"),
                )
              )
            )
          )
        )
      ).generateApis()
    }.message shouldBe "GraphQL type 'Todo' field 'id' is already declared"

    shouldThrow<IllegalArgumentException> {
      GraphQlApiGenerator(
        listOf(
          graphQlApi(
            typeDefinitions = mutableListOf(
              GraphQlTypeDefinitionToGenerateVo(
                kind = GraphQlTypeDefinitionKind.ENUM,
                name = "TodoStatus",
                enumValues = mutableListOf(
                  GraphQlEnumValueToGenerateVo("OPEN"),
                  GraphQlEnumValueToGenerateVo("OPEN"),
                ),
              )
            )
          )
        )
      ).generateApis()
    }.message shouldBe "GraphQL enum 'TodoStatus' value 'OPEN' is already declared"
  }

  @Test
  fun `should render GraphQL descriptions in generated schema`() {
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
                GraphQlArgumentToGenerateVo("id", Int::class.asTypeName(), "Int"),
              ),
              responseType = GraphQlGeneratorTestTodoDto::class.asTypeName(),
              responseGraphQlType = "Todo",
              description = "Find a single todo by id",
            )
          ),
          typeDefinitions = mutableListOf(
            GraphQlTypeDefinitionToGenerateVo(
              kind = GraphQlTypeDefinitionKind.TYPE,
              name = "Todo",
              fields = mutableListOf(GraphQlFieldToGenerateVo("id", "Int")),
              description = "A todo item\nwith multiple lines",
            ),
            GraphQlTypeDefinitionToGenerateVo(
              kind = GraphQlTypeDefinitionKind.SCALAR,
              name = "DateTime",
              description = "An ISO-8601 date-time",
            )
          )
        )
      )
    ).generateApis()

    val schema = result.schemas.single().content
    schema shouldContain "  \"\"\"Find a single todo by id\"\"\"\n  todo(id: Int!): Todo!"
    schema shouldContain "\"\"\"\nA todo item\nwith multiple lines\n\"\"\"\ntype Todo {"
    schema shouldContain "\"\"\"An ISO-8601 date-time\"\"\"\nscalar DateTime"
  }

  @Test
  fun `should reject blank GraphQL descriptions before rendering generated output`() {
    shouldThrow<IllegalArgumentException> {
      GraphQlApiGenerator(
        listOf(
          graphQlApi(functions = mutableListOf(todoQuery().apply { description = " " }))
        )
      ).generateApis()
    }.message shouldBe "GraphQL query field 'todo' description must not be blank"
  }

  @Test
  fun `should render GraphQL field descriptions in generated schema`() {
    val result = GraphQlApiGenerator(
      listOf(
        graphQlApi(
          typeDefinitions = mutableListOf(
            GraphQlTypeDefinitionToGenerateVo(
              kind = GraphQlTypeDefinitionKind.TYPE,
              name = "Todo",
              fields = mutableListOf(
                GraphQlFieldToGenerateVo("id", "Int", description = "Unique identifier"),
                GraphQlFieldToGenerateVo("description", "String"),
              ),
            ),
            GraphQlTypeDefinitionToGenerateVo(
              kind = GraphQlTypeDefinitionKind.INPUT,
              name = "TodoInput",
              fields = mutableListOf(
                GraphQlFieldToGenerateVo(
                  "description",
                  "String",
                  defaultValue = "\"new todo\"",
                  description = "Free text\nspanning lines",
                ),
              ),
            )
          )
        )
      )
    ).generateApis()

    val schema = result.schemas.single().content
    schema shouldContain "type Todo {\n  \"\"\"Unique identifier\"\"\"\n  id: Int!\n  description: String!\n}"
    schema shouldContain "input TodoInput {\n  \"\"\"\n  Free text\n  spanning lines\n  \"\"\"\n  description: String! = \"new todo\"\n}"
  }

  @Test
  fun `should reject blank GraphQL field descriptions before rendering generated output`() {
    shouldThrow<IllegalArgumentException> {
      GraphQlApiGenerator(
        listOf(
          graphQlApi(
            typeDefinitions = mutableListOf(
              GraphQlTypeDefinitionToGenerateVo(
                kind = GraphQlTypeDefinitionKind.TYPE,
                name = "Todo",
                fields = mutableListOf(GraphQlFieldToGenerateVo("id", "Int", description = " ")),
              )
            )
          )
        )
      ).generateApis()
    }.message shouldBe "GraphQL type 'Todo' field 'id' description must not be blank"
  }

  @Test
  fun `should render GraphQL argument descriptions in generated schema`() {
    val result = GraphQlApiGenerator(
      listOf(
        graphQlApi(
          functions = mutableListOf(
            GraphQlFunctionToGenerateVo(
              operation = GraphQlOperation.QUERY,
              functionName = "todo",
              arguments = mutableListOf(
                GraphQlArgumentToGenerateVo("id", Int::class.asTypeName(), "Int", description = "The todo id"),
                GraphQlArgumentToGenerateVo(
                  name = "filter",
                  type = GraphQlGeneratorTestTodoFilter::class.asTypeName(),
                  graphQlType = "TodoFilter",
                  nullable = true,
                  description = "Optional filter\nspanning lines",
                ),
                GraphQlArgumentToGenerateVo("limit", Int::class.asTypeName(), "Int", nullable = true),
              ),
              responseType = GraphQlGeneratorTestTodoDto::class.asTypeName(),
              responseGraphQlType = "Todo",
            )
          )
        )
      )
    ).generateApis()

    val schema = result.schemas.single().content
    schema shouldContain "  todo(\n" +
      "    \"\"\"The todo id\"\"\"\n" +
      "    id: Int!\n" +
      "    \"\"\"\n    Optional filter\n    spanning lines\n    \"\"\"\n" +
      "    filter: TodoFilter\n" +
      "    limit: Int\n" +
      "  ): Todo!"
  }

  @Test
  fun `should reject blank GraphQL argument descriptions before rendering generated output`() {
    shouldThrow<IllegalArgumentException> {
      GraphQlApiGenerator(
        listOf(
          graphQlApi(
            functions = mutableListOf(
              todoQuery(
                arguments = mutableListOf(
                  GraphQlArgumentToGenerateVo("id", Int::class.asTypeName(), "Int", description = " "),
                )
              )
            )
          )
        )
      ).generateApis()
    }.message shouldBe "GraphQL query field 'todo' argument 'id' description must not be blank"
  }

  @Test
  fun `should render GraphQL enum value descriptions in generated schema`() {
    val result = GraphQlApiGenerator(
      listOf(
        graphQlApi(
          typeDefinitions = mutableListOf(
            GraphQlTypeDefinitionToGenerateVo(
              kind = GraphQlTypeDefinitionKind.ENUM,
              name = "TodoStatus",
              enumValues = mutableListOf(
                GraphQlEnumValueToGenerateVo("OPEN", description = "Not yet done"),
                GraphQlEnumValueToGenerateVo("DONE", description = "Completed\nand archived"),
                GraphQlEnumValueToGenerateVo("CANCELLED"),
              ),
            )
          )
        )
      )
    ).generateApis()

    val schema = result.schemas.single().content
    schema shouldContain "enum TodoStatus {\n" +
      "  \"\"\"Not yet done\"\"\"\n" +
      "  OPEN\n" +
      "  \"\"\"\n  Completed\n  and archived\n  \"\"\"\n" +
      "  DONE\n" +
      "  CANCELLED\n" +
      "}"
  }

  @Test
  fun `should reject blank GraphQL enum value descriptions before rendering generated output`() {
    shouldThrow<IllegalArgumentException> {
      GraphQlApiGenerator(
        listOf(
          graphQlApi(
            typeDefinitions = mutableListOf(
              GraphQlTypeDefinitionToGenerateVo(
                kind = GraphQlTypeDefinitionKind.ENUM,
                name = "TodoStatus",
                enumValues = mutableListOf(GraphQlEnumValueToGenerateVo("OPEN", description = " ")),
              )
            )
          )
        )
      ).generateApis()
    }.message shouldBe "GraphQL enum 'TodoStatus' value 'OPEN' description must not be blank"
  }

  @Test
  fun `should render GraphQL field deprecation in generated schema`() {
    val result = GraphQlApiGenerator(
      listOf(
        graphQlApi(
          typeDefinitions = mutableListOf(
            GraphQlTypeDefinitionToGenerateVo(
              kind = GraphQlTypeDefinitionKind.TYPE,
              name = "Todo",
              fields = mutableListOf(
                GraphQlFieldToGenerateVo("id", "Int", deprecationReason = "Use uuid instead"),
                GraphQlFieldToGenerateVo("uuid", "String"),
              ),
            ),
            GraphQlTypeDefinitionToGenerateVo(
              kind = GraphQlTypeDefinitionKind.INPUT,
              name = "TodoInput",
              fields = mutableListOf(
                GraphQlFieldToGenerateVo("legacyTag", "String", nullable = true, deprecationReason = "Replaced by \"tags\""),
                GraphQlFieldToGenerateVo(
                  "description",
                  "String",
                  defaultValue = "\"new todo\"",
                  deprecationReason = "Will be removed",
                ),
              ),
            )
          )
        )
      )
    ).generateApis()

    val schema = result.schemas.single().content
    schema shouldContain "type Todo {\n  id: Int! @deprecated(reason: \"Use uuid instead\")\n  uuid: String!\n}"
    schema shouldContain "legacyTag: String @deprecated(reason: \"Replaced by \\\"tags\\\"\")"
    schema shouldContain "description: String! = \"new todo\" @deprecated(reason: \"Will be removed\")"
  }

  @Test
  fun `should reject blank GraphQL field deprecation reasons before rendering generated output`() {
    shouldThrow<IllegalArgumentException> {
      GraphQlApiGenerator(
        listOf(
          graphQlApi(
            typeDefinitions = mutableListOf(
              GraphQlTypeDefinitionToGenerateVo(
                kind = GraphQlTypeDefinitionKind.TYPE,
                name = "Todo",
                fields = mutableListOf(GraphQlFieldToGenerateVo("id", "Int", deprecationReason = " ")),
              )
            )
          )
        )
      ).generateApis()
    }.message shouldBe "GraphQL type 'Todo' field 'id' deprecation reason must not be blank"
  }

  @Test
  fun `should reject deprecation on required GraphQL input fields before rendering generated output`() {
    shouldThrow<IllegalArgumentException> {
      GraphQlApiGenerator(
        listOf(
          graphQlApi(
            typeDefinitions = mutableListOf(
              GraphQlTypeDefinitionToGenerateVo(
                kind = GraphQlTypeDefinitionKind.INPUT,
                name = "TodoInput",
                fields = mutableListOf(GraphQlFieldToGenerateVo("description", "String", deprecationReason = "gone")),
              )
            )
          )
        )
      ).generateApis()
    }.message shouldBe "GraphQL input 'TodoInput' field 'description' cannot be deprecated because it is a required input field"
  }

  @Test
  fun `should render GraphQL operation field argument and enum value deprecation in generated schema`() {
    val result = GraphQlApiGenerator(
      listOf(
        graphQlApi(
          functions = mutableListOf(
            GraphQlFunctionToGenerateVo(
              operation = GraphQlOperation.QUERY,
              functionName = "legacyTodos",
              arguments = mutableListOf(
                GraphQlArgumentToGenerateVo(
                  "limit",
                  Int::class.asTypeName(),
                  "Int",
                  nullable = true,
                  deprecationReason = "Use pagination",
                ),
                GraphQlArgumentToGenerateVo(
                  "status",
                  GraphQlGeneratorTestTodoStatus::class.asTypeName(),
                  "TodoStatus",
                  nullable = true,
                  defaultValue = "OPEN",
                  deprecationReason = "Filter \"removed\"",
                ),
              ),
              responseType = GraphQlGeneratorTestTodoDto::class.asTypeName(),
              responseGraphQlType = "Todo",
              responseCollection = true,
              deprecationReason = "Use todos",
            ),
            GraphQlFunctionToGenerateVo(
              operation = GraphQlOperation.QUERY,
              functionName = "describedTodo",
              arguments = mutableListOf(
                GraphQlArgumentToGenerateVo(
                  "id",
                  Int::class.asTypeName(),
                  "Int",
                  nullable = true,
                  description = "The todo id",
                  deprecationReason = "ids are unstable",
                ),
              ),
              responseType = GraphQlGeneratorTestTodoDto::class.asTypeName(),
              responseGraphQlType = "Todo",
            ),
          ),
          typeDefinitions = mutableListOf(
            GraphQlTypeDefinitionToGenerateVo(
              kind = GraphQlTypeDefinitionKind.ENUM,
              name = "TodoStatus",
              enumValues = mutableListOf(
                GraphQlEnumValueToGenerateVo("OPEN"),
                GraphQlEnumValueToGenerateVo("ARCHIVED", deprecationReason = "Use DONE"),
              ),
            )
          )
        )
      )
    ).generateApis()

    val schema = result.schemas.single().content
    schema shouldContain "  legacyTodos(limit: Int @deprecated(reason: \"Use pagination\"), " +
      "status: TodoStatus = OPEN @deprecated(reason: \"Filter \\\"removed\\\"\")): " +
      "[Todo!]! @deprecated(reason: \"Use todos\")"
    schema shouldContain "  describedTodo(\n" +
      "    \"\"\"The todo id\"\"\"\n" +
      "    id: Int @deprecated(reason: \"ids are unstable\")\n" +
      "  ): Todo!"
    schema shouldContain "enum TodoStatus {\n  OPEN\n  ARCHIVED @deprecated(reason: \"Use DONE\")\n}"
  }

  @Test
  fun `should reject deprecation on required GraphQL arguments before rendering generated output`() {
    shouldThrow<IllegalArgumentException> {
      GraphQlApiGenerator(
        listOf(
          graphQlApi(
            functions = mutableListOf(
              todoQuery(
                arguments = mutableListOf(
                  GraphQlArgumentToGenerateVo("id", Int::class.asTypeName(), "Int", deprecationReason = "gone"),
                )
              )
            )
          )
        )
      ).generateApis()
    }.message shouldBe "GraphQL query field 'todo' argument 'id' cannot be deprecated because it is a required argument"
  }

  @Test
  fun `should reject blank GraphQL operation field argument and enum value deprecation reasons before rendering generated output`() {
    shouldThrow<IllegalArgumentException> {
      GraphQlApiGenerator(
        listOf(graphQlApi(functions = mutableListOf(todoQuery().apply { deprecationReason = " " })))
      ).generateApis()
    }.message shouldBe "GraphQL query field 'todo' deprecation reason must not be blank"

    shouldThrow<IllegalArgumentException> {
      GraphQlApiGenerator(
        listOf(
          graphQlApi(
            functions = mutableListOf(
              todoQuery(
                arguments = mutableListOf(
                  GraphQlArgumentToGenerateVo("id", Int::class.asTypeName(), "Int", nullable = true, deprecationReason = " "),
                )
              )
            )
          )
        )
      ).generateApis()
    }.message shouldBe "GraphQL query field 'todo' argument 'id' deprecation reason must not be blank"

    shouldThrow<IllegalArgumentException> {
      GraphQlApiGenerator(
        listOf(
          graphQlApi(
            typeDefinitions = mutableListOf(
              GraphQlTypeDefinitionToGenerateVo(
                kind = GraphQlTypeDefinitionKind.ENUM,
                name = "TodoStatus",
                enumValues = mutableListOf(GraphQlEnumValueToGenerateVo("OPEN", deprecationReason = " ")),
              )
            )
          )
        )
      ).generateApis()
    }.message shouldBe "GraphQL enum 'TodoStatus' value 'OPEN' deprecation reason must not be blank"
  }

  private fun graphQlApi(
    apiName: String = "TodoGraphQl",
    functions: MutableList<GraphQlFunctionToGenerateVo> = mutableListOf(),
    typeDefinitions: MutableList<GraphQlTypeDefinitionToGenerateVo> = mutableListOf(),
  ): GraphQlApiToGenerateVo {
    return GraphQlApiToGenerateVo(
      controllerPackage = "com.example.graphql",
      serviceInterfacePackage = "com.example.graphql.service",
      apiName = apiName,
      functions = functions,
      typeDefinitions = typeDefinitions,
    )
  }

  private fun todoQuery(
    functionName: String = "todo",
    arguments: MutableList<GraphQlArgumentToGenerateVo> = mutableListOf(),
  ): GraphQlFunctionToGenerateVo {
    return GraphQlFunctionToGenerateVo(
      operation = GraphQlOperation.QUERY,
      functionName = functionName,
      arguments = arguments,
      responseType = GraphQlGeneratorTestTodoDto::class.asTypeName(),
      responseGraphQlType = "Todo",
    )
  }
}
