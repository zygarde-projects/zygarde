package zygarde.test

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForObject
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import zygarde.test.dao.SequenceIntAuthorDao
import zygarde.test.dao.SequenceLongBookDao
import zygarde.test.entity.SequenceIntAuthor
import zygarde.test.entity.SequenceLongBook

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = [ZygardeJpaTestApplication::class])
@ActiveProfiles("test", "test-oracle")
@DirtiesContext
class AuditedSequenceIdEntityTest {

  @Autowired
  lateinit var sequenceIntAuthorDao: SequenceIntAuthorDao

  @Autowired
  lateinit var sequenceLongBookDao: SequenceLongBookDao

  @Autowired
  lateinit var jdbcTemplate: JdbcTemplate

  @Test
  fun `sequence int id with audited`() {
    val author = sequenceIntAuthorDao.save(SequenceIntAuthor("author 1"))
    jdbcTemplate.queryForObject<Long>("SELECT count(id) from sequence_int_author_aud") shouldBe 1L

    author.also { it.name = "author 1 edited" }.let(sequenceIntAuthorDao::saveAndFlush)

    jdbcTemplate.queryForObject<Long>("SELECT count(id) from sequence_int_author_aud") shouldBe 2L
  }

  @Test
  fun `sequence long id with audited`() {
    val book = sequenceLongBookDao.save(SequenceLongBook("book 1"))
    jdbcTemplate.queryForObject<Long>("SELECT count(id) from sequence_long_book_aud") shouldBe 1L

    book.also { it.name = "book 1 edited" }.let(sequenceLongBookDao::saveAndFlush)

    jdbcTemplate.queryForObject<Long>("SELECT count(id) from sequence_long_book_aud") shouldBe 2L
  }
}
