package example.test

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.graphql.test.tester.GraphQlTester
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureGraphQlTester
@ActiveProfiles("test")
class BookGraphQlTest(
  @Autowired private val graphQlTester: GraphQlTester,
) {
  @Test
  fun `book graphql relation and filter test`() {
    val authorId = graphQlTester.document(
      """
      mutation {
        createAuthor(input: { name: "GraphQL Author A" }) {
          id
        }
      }
      """.trimIndent()
    )
      .execute()
      .path("createAuthor.id")
      .entity(String::class.java)
      .get()
      .toInt()

    graphQlTester.document(
      """
      mutation {
        createAuthor(input: { name: "GraphQL Author B" }) {
          id
        }
      }
      """.trimIndent()
    )
      .execute()
      .path("createAuthor.id")
      .entity(Int::class.java)

    listOf("GraphQL Book One", "GraphQL Book Two").forEach { title ->
      graphQlTester.document(
        """
        mutation {
          createBook(input: { title: "$title", authorId: $authorId }) {
            id
            author {
              name
            }
          }
        }
        """.trimIndent()
      )
        .execute()
        .path("createBook.author.name")
        .entity(String::class.java)
        .isEqualTo("GraphQL Author A")
    }

    graphQlTester.document(
      """
      query {
        books(filter: { authorNameContains: "Author A" }) {
          title
          author {
            name
          }
        }
      }
      """.trimIndent()
    )
      .execute()
      .path("books[*].title")
      .entityList(String::class.java)
      .containsExactly("GraphQL Book One", "GraphQL Book Two")
      .path("books[*].author.name")
      .entityList(String::class.java)
      .containsExactly("GraphQL Author A", "GraphQL Author A")
  }
}
