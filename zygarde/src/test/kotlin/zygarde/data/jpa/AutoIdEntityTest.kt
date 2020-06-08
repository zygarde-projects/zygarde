package zygarde.data.jpa

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import zygarde.data.jpa.dao.TestAuthorDao
import zygarde.data.jpa.dao.TestAuthorGroupDao
import zygarde.data.jpa.dao.TestBookDao
import zygarde.data.jpa.entity.Author
import zygarde.data.jpa.entity.AuthorGroup
import zygarde.data.jpa.entity.Book
import zygarde.data.jpa.entity.getId
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = [ZygardeJpaTestApplication::class])
@ActiveProfiles("test")
@DirtiesContext
class AutoIdEntityTest {

  @Autowired
  lateinit var groupDao: TestAuthorGroupDao

  @Autowired
  lateinit var authorDao: TestAuthorDao

  @Autowired
  lateinit var bookDao: TestBookDao

  class Foo(val id: Int, val name: String = "")

  @Test
  fun `should able to save and increase id`() {
    val authorGroup = groupDao.save(AuthorGroup()).also {
      it.id shouldBe 1
      it.getId() shouldBe 1
    }
    val author = authorDao.save(Author()).also {
      it.id shouldBe 1
      it.getId() shouldBe 1
    }
    Objects.equals(authorGroup, null) shouldBe false
    Objects.equals(authorGroup, author) shouldBe false
    Objects.equals(authorGroup, Foo(id = 1, name = "")) shouldBe false
  }

  @Test
  fun `should able to save list and increase id`() {
    val books = bookDao.saveAll(
      setOf(Book(), Book())
    )
    books[0].getId() + 1 shouldBe books[1].getId()

    (books[0] == books[1]) shouldBe false
    books.toSet().size shouldBe 2

    (Book() == books[0]) shouldBe false
  }
}
