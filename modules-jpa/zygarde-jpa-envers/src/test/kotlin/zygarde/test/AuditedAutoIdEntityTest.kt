package zygarde.test

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForObject
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import zygarde.test.dao.AutoIntAuthorDao
import zygarde.test.dao.AutoLongBookDao
import zygarde.test.entity.AutoIntAuthor
import zygarde.test.entity.AutoLongBook

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = [ZygardeJpaTestApplication::class])
@ActiveProfiles("test")
@DirtiesContext
class AuditedAutoIdEntityTest {

  @Autowired
  lateinit var autoIntAuthorDao: AutoIntAuthorDao

  @Autowired
  lateinit var autoLongBookDao: AutoLongBookDao

  @Autowired
  lateinit var jdbcTemplate: JdbcTemplate

  @Test
  fun `auto int id with audited`() {
    val author = autoIntAuthorDao.save(AutoIntAuthor("author 1"))
    jdbcTemplate.queryForObject<Long>("SELECT count(id) from auto_int_author_aud") shouldBe 1L

    author.also { it.name = "author 1 edited" }.let(autoIntAuthorDao::saveAndFlush)

    jdbcTemplate.queryForObject<Long>("SELECT count(id) from auto_int_author_aud") shouldBe 2L
  }

  @Test
  fun `auto long id with audited`() {
    val book = autoLongBookDao.save(AutoLongBook("book 1"))
    jdbcTemplate.queryForObject<Long>("SELECT count(id) from auto_long_book_aud") shouldBe 1L

    book.also { it.name = "book 1 edited" }.let(autoLongBookDao::saveAndFlush)

    jdbcTemplate.queryForObject<Long>("SELECT count(id) from auto_long_book_aud") shouldBe 2L
  }
}
