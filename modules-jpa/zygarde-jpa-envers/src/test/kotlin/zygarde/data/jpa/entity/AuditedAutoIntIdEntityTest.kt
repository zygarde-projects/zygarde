package zygarde.data.jpa.entity

import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import zygarde.test.ZygardeJpaTestApplication
import zygarde.test.dao.AutoIntAuthorDao
import zygarde.test.entity.AutoIntAuthor

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = [ZygardeJpaTestApplication::class])
@ActiveProfiles("test")
@DirtiesContext
class AuditedAutoIntIdEntityTest {

  @Autowired
  lateinit var autoIntAuthorDao: AutoIntAuthorDao

  @Test
  fun `should implement AutoIdGetter interface with Int type`() {
    val author = autoIntAuthorDao.save(AutoIntAuthor("Test"))

    author.shouldBeInstanceOf<AutoIdGetter<Int>>()
    author.id.shouldBeInstanceOf<Int>()
  }

  @Test
  fun `should properly inherit from AuditedAutoIdEntity with Int type`() {
    val author = autoIntAuthorDao.save(AutoIntAuthor("Test"))

    author.shouldBeInstanceOf<AuditedAutoIdEntity<Int>>()
  }

  @Test
  fun `should generate valid Int IDs`() {
    val author1 = autoIntAuthorDao.save(AutoIntAuthor("Author 1"))
    val author2 = autoIntAuthorDao.save(AutoIntAuthor("Author 2"))

    author1.id.shouldBeInstanceOf<Int>()
    author2.id.shouldBeInstanceOf<Int>()
    author1.id shouldNotBe author2.id
  }
}
