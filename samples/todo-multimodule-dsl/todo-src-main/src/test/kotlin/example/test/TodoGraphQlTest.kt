package example.test

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.graphql.test.tester.GraphQlTester
import org.springframework.test.context.ActiveProfiles
import zygarde.data.provider.DataProviderContext
import zygarde.data.provider.DataProviderContextResolver

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureGraphQlTester
@ActiveProfiles("test")
class TodoGraphQlTest(
  @Autowired private val graphQlTester: GraphQlTester,
) {
  @TestConfiguration
  class DataProviderContextResolverTestConfig {
    @Bean
    fun dataProviderContextResolver(): DataProviderContextResolver =
      object : DataProviderContextResolver {
        override fun resolve(): DataProviderContext =
          DataProviderContext(mapOf("fileNameMarker" to "graphql-test"))
      }
  }

  @Test
  fun `todo graphql crud and filter test`() {
    graphQlTester.document(
      """
      mutation {
        createTodo(input: { description: "first graphql todo" }) {
          id
          description
        }
      }
      """.trimIndent()
    )
      .execute()
      .path("createTodo.description")
      .entity(String::class.java)
      .isEqualTo("first graphql todo")

    val secondId = graphQlTester.document(
      """
      mutation {
        createTodo(input: { description: "second graphql todo" }) {
          id
        }
      }
      """.trimIndent()
    )
      .execute()
      .path("createTodo.id")
      .entity(Int::class.java)
      .get()

    graphQlTester.document(
      """
      query {
        todos(filter: { descriptionContains: "second" }) {
          id
          description
        }
      }
      """.trimIndent()
    )
      .execute()
      .path("todos[*].description")
      .entityList(String::class.java)
      .containsExactly("second graphql todo")

    graphQlTester.document(
      """
      query {
        todos(filter: { idsIn: [$secondId] }) {
          id
          description
        }
      }
      """.trimIndent()
    )
      .execute()
      .path("todos[*].description")
      .entityList(String::class.java)
      .containsExactly("second graphql todo")

    graphQlTester.document(
      """
      query {
        todosByIds(ids: [$secondId]) {
          id
          description
          file {
            id
            name
          }
        }
      }
      """.trimIndent()
    )
      .execute()
      .path("todosByIds[*].description")
      .entityList(String::class.java)
      .containsExactly("second graphql todo")

    graphQlTester.document(
      """
      query {
        todos(filter: { idsIn: [$secondId] }) {
          file {
            id
            name
          }
        }
      }
      """.trimIndent()
    )
      .execute()
      .path("todos[*].file.name")
      .entityList(String::class.java)
      .containsExactly("File todo-file [graphql-test]")

    graphQlTester.document(
      """
      mutation {
        updateTodo(id: $secondId, input: { description: "updated graphql todo" }) {
          description
        }
      }
      """.trimIndent()
    )
      .execute()
      .path("updateTodo.description")
      .entity(String::class.java)
      .isEqualTo("updated graphql todo")

    graphQlTester.document(
      """
      query {
        todo(id: $secondId) {
          description
        }
      }
      """.trimIndent()
    )
      .execute()
      .path("todo.description")
      .entity(String::class.java)
      .isEqualTo("updated graphql todo")

    graphQlTester.document(
      """
      mutation {
        deleteTodo(id: $secondId)
      }
      """.trimIndent()
    )
      .execute()
      .path("deleteTodo")
      .entity(Boolean::class.java)
      .isEqualTo(true)

    graphQlTester.document(
      """
      query {
        todo(id: $secondId) {
          description
        }
      }
      """.trimIndent()
    )
      .execute()
      .path("todo")
      .valueIsNull()

    val remainingCount = graphQlTester.document(
      """
      query {
        todos {
          id
        }
      }
      """.trimIndent()
    )
      .execute()
      .path("todos[*].id")
      .entityList(Int::class.java)
      .get()
      .size

    remainingCount shouldBe 1
  }
}
