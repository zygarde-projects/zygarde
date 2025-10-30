package zygarde.data.jpa.entity

import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import zygarde.test.ZygardeJpaTestApplication
import zygarde.test.dao.SequenceIntAuthorDao
import zygarde.test.entity.SequenceIntAuthor

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = [ZygardeJpaTestApplication::class])
@ActiveProfiles("test", "test-oracle")
@DirtiesContext
class AuditedSequenceIntIdEntityTest {

  @Autowired
  lateinit var sequenceIntAuthorDao: SequenceIntAuthorDao

  @Test
  fun `should implement AutoIdGetter interface with Int type for sequence`() {
    val author = sequenceIntAuthorDao.save(SequenceIntAuthor("Test"))

    author.shouldBeInstanceOf<AutoIdGetter<Int>>()
    author.id.shouldBeInstanceOf<Int>()
  }

  @Test
  fun `should properly inherit from AuditedSequenceIdEntity with Int type`() {
    val author = sequenceIntAuthorDao.save(SequenceIntAuthor("Test"))

    author.shouldBeInstanceOf<AuditedSequenceIdEntity<Int>>()
  }

  @Test
  fun `should generate valid sequence Int IDs`() {
    val author1 = sequenceIntAuthorDao.save(SequenceIntAuthor("Author 1"))
    val author2 = sequenceIntAuthorDao.save(SequenceIntAuthor("Author 2"))

    author1.id.shouldBeInstanceOf<Int>()
    author2.id.shouldBeInstanceOf<Int>()
    author1.id shouldNotBe author2.id
  }
}
