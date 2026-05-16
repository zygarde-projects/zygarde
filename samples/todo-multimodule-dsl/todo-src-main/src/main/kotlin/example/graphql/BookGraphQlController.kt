package example.graphql

import example.GraphQlAuthor
import example.GraphQlBook
import example.codegen.data.dao.GraphQlAuthorDao
import example.codegen.data.dao.GraphQlBookDao
import example.codegen.data.dao.search
import example.codegen.entity.search.author
import example.codegen.entity.search.name
import example.codegen.entity.search.title
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.BatchMapping
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class BookGraphQlController(
  @Autowired private val authorDao: GraphQlAuthorDao,
  @Autowired private val bookDao: GraphQlBookDao,
) {
  @QueryMapping
  fun authors(): Collection<GraphQlAuthor> {
    return authorDao.findAll()
  }

  @QueryMapping
  fun books(
    @Argument filter: BookFilter?,
  ): Collection<GraphQlBook> {
    return bookDao.search {
      title() contains filter?.titleContains
      author().name() contains filter?.authorNameContains
    }
  }

  @MutationMapping
  fun createAuthor(
    @Argument input: AuthorInput,
  ): GraphQlAuthor {
    return authorDao.saveAndFlush(GraphQlAuthor(name = input.name))
  }

  @MutationMapping
  fun createBook(
    @Argument input: BookInput,
  ): GraphQlBook {
    return bookDao.saveAndFlush(
      GraphQlBook(
        title = input.title,
        authorId = input.authorId,
      )
    )
  }

  @BatchMapping(typeName = "Book", field = "author")
  fun author(books: List<GraphQlBook>): Map<GraphQlBook, GraphQlAuthor> {
    val authorIds = books.mapNotNull { it.authorId }.distinct()
    val authorsById = authorDao.findAllById(authorIds).associateBy { it.id }
    return books.mapNotNull { book ->
      val author = book.authorId?.let(authorsById::get)
      author?.let { book to it }
    }.toMap()
  }
}

data class AuthorInput(
  val name: String,
)

data class BookInput(
  val title: String,
  val authorId: Int,
)

data class BookFilter(
  val titleContains: String? = null,
  val authorNameContains: String? = null,
)
