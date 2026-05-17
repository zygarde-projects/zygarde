package zygarde.codegen.dsl.graphql

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import zygarde.codegen.model.graphql.GraphQlEnumValueToGenerateVo
import zygarde.codegen.model.graphql.GraphQlOperation
import zygarde.codegen.model.graphql.GraphQlTypeDefinitionKind
import zygarde.core.annotation.Comment

class GraphQlDslCodegenTest {
  class Todo {
    var id: Int? = null

    @Comment("what needs to be done")
    var description: String = ""
    var status: TodoStatus = TodoStatus.OPEN
  }

  data class TodoDto(val id: Int, val description: String)

  data class TodoInput(val description: String)

  data class TodoFilter(val descriptionContains: String?)

  data class AuthorDto(val id: Int, val name: String)

  data class BookDto(val id: Int, val title: String)

  class Author {
    var id: Int? = null
    var name: String = ""
  }

  class Book {
    var id: Int? = null
    var title: String = ""
  }

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
    api.typeDefinitions[2].enumValues shouldBe mutableListOf(
      GraphQlEnumValueToGenerateVo("OPEN"),
      GraphQlEnumValueToGenerateVo("DONE"),
    )
    api.typeDefinitions[3].kind shouldBe GraphQlTypeDefinitionKind.SCALAR
    api.typeDefinitions[3].name shouldBe "Long"
  }

  @Test
  fun `should derive GraphQL type definitions from entity projections and bind operation types`() {
    val dsl = object : GraphQlDslCodegen() {
      override fun codegen() {
        schema("TodoGraphQl") {
          type<TodoDto>("Todo") {
            fromAutoIntId(Todo::id)
            from(Todo::description, Todo::status)
          }
          input<TodoInput>("TodoInput") {
            applyTo(Todo::description)
          }
          mutation("createTodo") {
            argument<TodoInput>("input")
            returns<TodoDto>()
            serviceName = "TodoGraphQlService"
          }
        }
      }
    }

    dsl.codegen()

    val api = dsl.apisToGenerate.single()
    api.functions.single().apply {
      arguments.single().graphQlType shouldBe "TodoInput"
      responseGraphQlType shouldBe "Todo"
    }
    api.typeDefinitions.map { it.name } shouldBe listOf("Todo", "TodoStatus", "TodoInput")
    api.typeDefinitions.single { it.name == "Todo" }.apply {
      kind shouldBe GraphQlTypeDefinitionKind.TYPE
      fields.map { it.name } shouldBe listOf("id", "description", "status")
      fields.single { it.name == "id" }.apply {
        graphQlType shouldBe "ID"
        nullable shouldBe false
      }
      fields.single { it.name == "description" }.apply {
        graphQlType shouldBe "String"
        description shouldBe "what needs to be done"
      }
      fields.single { it.name == "status" }.graphQlType shouldBe "TodoStatus"
    }
    api.typeDefinitions.single { it.name == "TodoInput" }.apply {
      kind shouldBe GraphQlTypeDefinitionKind.INPUT
      fields.single().name shouldBe "description"
    }
  }

  @Test
  fun `should add nullable nested reference fields to entity projections`() {
    val dsl = object : GraphQlDslCodegen() {
      override fun codegen() {
        schema("BookGraphQl") {
          type<AuthorDto>("Author") {
            fromAutoIntId(Author::id)
            from(Author::name)
          }
          type<BookDto>("Book") {
            fromAutoIntId(Book::id)
            from(Book::title)
            ref<AuthorDto>("author", nullable = true)
          }
        }
      }
    }

    dsl.codegen()

    dsl.apisToGenerate.single()
      .typeDefinitions.single { it.name == "Book" }
      .fields.single { it.name == "author" }
      .apply {
        graphQlType shouldBe "Author"
        nullable shouldBe true
        collection shouldBe false
      }
  }

  @Test
  fun `should add nested reference collection fields to entity projections`() {
    val dsl = object : GraphQlDslCodegen() {
      override fun codegen() {
        schema("BookGraphQl") {
          bindGraphQlType<BookDto>("Book")
          type<AuthorDto>("Author") {
            fromAutoIntId(Author::id)
            from(Author::name)
            refCollection<BookDto>("books")
          }
        }
      }
    }

    dsl.codegen()

    dsl.apisToGenerate.single()
      .typeDefinitions.single { it.name == "Author" }
      .fields.single { it.name == "books" }
      .apply {
        graphQlType shouldBe "Book"
        nullable shouldBe false
        collection shouldBe true
        itemNullable shouldBe false
      }
  }

  @Test
  fun `should support bidirectional nested references with pre-registered GraphQL type names`() {
    val dsl = object : GraphQlDslCodegen() {
      override fun codegen() {
        schema("BookGraphQl") {
          bindGraphQlType<AuthorDto>("Author")
          bindGraphQlType<BookDto>("Book")

          type<AuthorDto>("Author") {
            fromAutoIntId(Author::id)
            from(Author::name)
            refCollection<BookDto>("books")
          }
          type<BookDto>("Book") {
            fromAutoIntId(Book::id)
            from(Book::title)
            ref<AuthorDto>("author", nullable = true)
          }
        }
      }
    }

    dsl.codegen()

    val api = dsl.apisToGenerate.single()
    api.typeDefinitions.single { it.name == "Author" }
      .fields.single { it.name == "books" }
      .graphQlType shouldBe "Book"
    api.typeDefinitions.single { it.name == "Book" }
      .fields.single { it.name == "author" }
      .graphQlType shouldBe "Author"
  }

  @Test
  fun `should fail fast when nested reference type is not bound`() {
    val ex = shouldThrow<IllegalArgumentException> {
      object : GraphQlDslCodegen() {
        override fun codegen() {
          schema("BookGraphQl") {
            type<BookDto>("Book") {
              fromAutoIntId(Book::id)
              from(Book::title)
              ref<AuthorDto>("author")
            }
          }
        }
      }.codegen()
    }

    ex.message shouldContain "requires a GraphQL type binding"
    ex.message shouldContain "AuthorDto"
  }

  @Test
  fun `should allow explicit nested reference GraphQL type names without binding`() {
    val dsl = object : GraphQlDslCodegen() {
      override fun codegen() {
        schema("BookGraphQl") {
          type<BookDto>("Book") {
            fromAutoIntId(Book::id)
            from(Book::title)
            ref("author", graphQlType = "Author", nullable = true)
          }
        }
      }
    }

    dsl.codegen()

    dsl.apisToGenerate.single()
      .typeDefinitions.single { it.name == "Book" }
      .fields.single { it.name == "author" }
      .apply {
        graphQlType shouldBe "Author"
        nullable shouldBe true
      }
  }

  @Test
  fun `should carry GraphQL descriptions through the DSL`() {
    val dsl = object : GraphQlDslCodegen() {
      override fun codegen() {
        schema("TodoGraphQl") {
          query("todo") {
            argument<Int>("id", description = "The todo id")
            collectionArgument<Int>("tags", nullable = true, description = "Optional tag filter")
            returns<TodoDto>("Todo")
            description = "Find a single todo by id"
          }
          type("Todo") {
            description = "A todo item"
            field<Int>("id", description = "Unique identifier")
            collectionField<String>("tags", description = "Free-form labels")
          }
          scalar<Long>(description = "A 64-bit integer scalar")
        }
      }
    }

    dsl.codegen()

    val api = dsl.apisToGenerate.single()
    api.functions.single().description shouldBe "Find a single todo by id"
    api.functions.single().arguments[0].description shouldBe "The todo id"
    api.functions.single().arguments[1].description shouldBe "Optional tag filter"
    api.typeDefinitions[0].description shouldBe "A todo item"
    api.typeDefinitions[0].fields[0].description shouldBe "Unique identifier"
    api.typeDefinitions[0].fields[1].description shouldBe "Free-form labels"
    api.typeDefinitions[1].description shouldBe "A 64-bit integer scalar"
  }

  @Test
  fun `should reject blank GraphQL descriptions`() {
    shouldThrow<IllegalArgumentException> {
      object : GraphQlDslCodegen() {
        override fun codegen() {
          schema("TodoGraphQl") {
            query("todo") {
              returns<TodoDto>("Todo")
              description = " "
            }
          }
        }
      }.codegen()
    }.message shouldBe "GraphQL query field 'todo' description must not be blank"

    shouldThrow<IllegalArgumentException> {
      object : GraphQlDslCodegen() {
        override fun codegen() {
          schema("TodoGraphQl") {
            scalar("DateTime", description = "")
          }
        }
      }.codegen()
    }.message shouldBe "GraphQL scalar 'DateTime' description must not be blank"

    shouldThrow<IllegalArgumentException> {
      DslGraphQlTypeDefinition.type("Todo").field<Int>("id", description = " ")
    }.message shouldBe "GraphQL type 'Todo' field 'id' description must not be blank"

    shouldThrow<IllegalArgumentException> {
      DslGraphQlFunction("todo", GraphQlOperation.QUERY).argument<Int>("id", description = " ")
    }.message shouldBe "GraphQL query field 'todo' argument 'id' description must not be blank"

    shouldThrow<IllegalArgumentException> {
      DslGraphQlTypeDefinition.enumType("TodoStatus").value("OPEN", description = " ")
    }.message shouldBe "GraphQL enum 'TodoStatus' value 'OPEN' description must not be blank"
  }

  @Test
  fun `should carry GraphQL enum value descriptions through the DSL`() {
    val dsl = object : GraphQlDslCodegen() {
      override fun codegen() {
        schema("TodoGraphQl") {
          enumType("TodoStatus") {
            description = "Lifecycle state of a todo"
            value("OPEN", description = "Not yet done")
            value("DONE")
          }
        }
      }
    }

    dsl.codegen()

    val enumType = dsl.apisToGenerate.single().typeDefinitions.single()
    enumType.description shouldBe "Lifecycle state of a todo"
    enumType.enumValues shouldBe mutableListOf(
      GraphQlEnumValueToGenerateVo("OPEN", description = "Not yet done"),
      GraphQlEnumValueToGenerateVo("DONE"),
    )
  }

  @Test
  fun `should carry GraphQL field deprecation reasons through the DSL`() {
    val dsl = object : GraphQlDslCodegen() {
      override fun codegen() {
        schema("TodoGraphQl") {
          type("Todo") {
            field<Int>("id", deprecationReason = "Use uuid instead")
            field<String>("uuid")
          }
          input("TodoInput") {
            field<String>("legacyTag", nullable = true, deprecationReason = "Replaced by tags")
            field<String>(
              "source",
              defaultValue = GraphQlDefaultValue.string("manual"),
              deprecationReason = "No longer tracked",
            )
          }
        }
      }
    }

    dsl.codegen()

    val api = dsl.apisToGenerate.single()
    api.typeDefinitions[0].fields[0].deprecationReason shouldBe "Use uuid instead"
    api.typeDefinitions[0].fields[1].deprecationReason shouldBe null
    api.typeDefinitions[1].fields[0].deprecationReason shouldBe "Replaced by tags"
    api.typeDefinitions[1].fields[1].deprecationReason shouldBe "No longer tracked"
  }

  @Test
  fun `should reject blank GraphQL field deprecation reasons`() {
    shouldThrow<IllegalArgumentException> {
      DslGraphQlTypeDefinition.type("Todo").field<Int>("id", deprecationReason = " ")
    }.message shouldBe "GraphQL type 'Todo' field 'id' deprecation reason must not be blank"
  }

  @Test
  fun `should reject deprecation on required GraphQL input fields`() {
    shouldThrow<IllegalArgumentException> {
      DslGraphQlTypeDefinition.input("TodoInput").field<String>("description", deprecationReason = "gone")
    }.message shouldBe "GraphQL input 'TodoInput' field 'description' cannot be deprecated because it is a required input field"

    shouldThrow<IllegalArgumentException> {
      DslGraphQlTypeDefinition.input("TodoInput")
        .collectionField<String>("tags", deprecationReason = "gone")
    }.message shouldBe "GraphQL input 'TodoInput' field 'tags' cannot be deprecated because it is a required input field"
  }

  @Test
  fun `should carry GraphQL operation field argument and enum value deprecation reasons through the DSL`() {
    val dsl = object : GraphQlDslCodegen() {
      override fun codegen() {
        schema("TodoGraphQl") {
          query("legacyTodos") {
            argument<Int>("limit", nullable = true, deprecationReason = "Use pagination")
            collectionArgument<Int>("ids", nullable = true, deprecationReason = "Use cursors")
            argument<Int>("offset", defaultValue = GraphQlDefaultValue.int(0))
            returnsCollection<TodoDto>("Todo")
            deprecationReason = "Use todos"
          }
          enumType("TodoStatus") {
            value("OPEN")
            value("ARCHIVED", deprecationReason = "Use DONE")
          }
        }
      }
    }

    dsl.codegen()

    val api = dsl.apisToGenerate.single()
    api.functions.single().deprecationReason shouldBe "Use todos"
    api.functions.single().arguments[0].deprecationReason shouldBe "Use pagination"
    api.functions.single().arguments[1].deprecationReason shouldBe "Use cursors"
    api.functions.single().arguments[2].deprecationReason shouldBe null
    api.typeDefinitions.single().enumValues shouldBe mutableListOf(
      GraphQlEnumValueToGenerateVo("OPEN"),
      GraphQlEnumValueToGenerateVo("ARCHIVED", deprecationReason = "Use DONE"),
    )
  }

  @Test
  fun `should reject deprecation on required GraphQL arguments`() {
    shouldThrow<IllegalArgumentException> {
      DslGraphQlFunction("todo", GraphQlOperation.QUERY).argument<Int>("id", deprecationReason = "gone")
    }.message shouldBe "GraphQL query field 'todo' argument 'id' cannot be deprecated because it is a required argument"

    shouldThrow<IllegalArgumentException> {
      DslGraphQlFunction("todo", GraphQlOperation.QUERY).collectionArgument<Int>("ids", deprecationReason = "gone")
    }.message shouldBe "GraphQL query field 'todo' argument 'ids' cannot be deprecated because it is a required argument"
  }

  @Test
  fun `should reject blank GraphQL operation field argument and enum value deprecation reasons`() {
    shouldThrow<IllegalArgumentException> {
      object : GraphQlDslCodegen() {
        override fun codegen() {
          schema("TodoGraphQl") {
            query("todo") {
              returns<TodoDto>("Todo")
              deprecationReason = " "
            }
          }
        }
      }.codegen()
    }.message shouldBe "GraphQL query field 'todo' deprecation reason must not be blank"

    shouldThrow<IllegalArgumentException> {
      DslGraphQlFunction("todo", GraphQlOperation.QUERY).argument<Int>("id", nullable = true, deprecationReason = " ")
    }.message shouldBe "GraphQL query field 'todo' argument 'id' deprecation reason must not be blank"

    shouldThrow<IllegalArgumentException> {
      DslGraphQlTypeDefinition.enumType("TodoStatus").value("OPEN", deprecationReason = " ")
    }.message shouldBe "GraphQL enum 'TodoStatus' value 'OPEN' deprecation reason must not be blank"
  }

  @Test
  fun `should reject default values on object type fields`() {
    shouldThrow<IllegalArgumentException> {
      DslGraphQlTypeDefinition.type("Todo")
        .field<String>("description", defaultValue = "\"open\"")
    }.message shouldBe "GraphQL field default values are only supported on input fields"
  }

  @Test
  fun `should reject empty GraphQL type definitions`() {
    shouldThrow<IllegalArgumentException> {
      DslGraphQlTypeDefinition.type("Todo").toGraphQlTypeDefinitionToGenerateVo()
    }.message shouldBe "GraphQL type 'Todo' must declare at least one field"

    shouldThrow<IllegalArgumentException> {
      DslGraphQlTypeDefinition.input("TodoInput").toGraphQlTypeDefinitionToGenerateVo()
    }.message shouldBe "GraphQL input 'TodoInput' must declare at least one field"

    shouldThrow<IllegalArgumentException> {
      DslGraphQlTypeDefinition.enumType("TodoStatus").toGraphQlTypeDefinitionToGenerateVo()
    }.message shouldBe "GraphQL enum 'TodoStatus' must declare at least one value"
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

  @Test
  fun `should carry GraphQL union types through the DSL`() {
    val dsl = object : GraphQlDslCodegen() {
      override fun codegen() {
        schema("TodoGraphQl") {
          type("Book") { field<String>("title") }
          type("Author") { field<String>("name") }
          union("SearchResult", "Book", "Author", description = "Either a book or an author")
          union("Listed", listOf("Book"))
        }
      }
    }

    dsl.codegen()

    val typeDefinitions = dsl.apisToGenerate.single().typeDefinitions
    val searchResult = typeDefinitions.single { it.name == "SearchResult" }
    searchResult.kind shouldBe GraphQlTypeDefinitionKind.UNION
    searchResult.unionMemberTypes shouldBe mutableListOf("Book", "Author")
    searchResult.description shouldBe "Either a book or an author"
    typeDefinitions.single { it.name == "Listed" }.unionMemberTypes shouldBe mutableListOf("Book")
  }

  @Test
  fun `should reject GraphQL union types without member types`() {
    shouldThrow<IllegalArgumentException> {
      object : GraphQlDslCodegen() {
        override fun codegen() {
          schema("TodoGraphQl") {
            union("SearchResult")
          }
        }
      }.codegen()
    }.message shouldBe "GraphQL union 'SearchResult' must declare at least one member type"
  }

  @Test
  fun `should reject duplicate GraphQL union member types`() {
    shouldThrow<IllegalArgumentException> {
      object : GraphQlDslCodegen() {
        override fun codegen() {
          schema("TodoGraphQl") {
            union("SearchResult", "Book", "Book")
          }
        }
      }.codegen()
    }.message shouldBe "GraphQL union 'SearchResult' member type 'Book' is already declared"
  }
}
