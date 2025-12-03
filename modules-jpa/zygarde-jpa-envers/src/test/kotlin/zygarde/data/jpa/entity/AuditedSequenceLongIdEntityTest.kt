package zygarde.data.jpa.entity

import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import zygarde.test.ZygardeJpaTestApplication
import zygarde.test.dao.SequenceLongBookDao
import zygarde.test.entity.SequenceLongBook

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = [ZygardeJpaTestApplication::class])
@ActiveProfiles("test", "test-oracle")
@DirtiesContext
class AuditedSequenceLongIdEntityTest {
  @Autowired
  lateinit var sequenceLongBookDao: SequenceLongBookDao

  @Test
  fun `should implement AutoIdGetter interface with Long type for sequence`() {
    val book = sequenceLongBookDao.save(SequenceLongBook("Test"))

    book.shouldBeInstanceOf<AutoIdGetter<Long>>()
    book.id.shouldBeInstanceOf<Long>()
  }

  @Test
  fun `should properly inherit from AuditedSequenceIdEntity with Long type`() {
    val book = sequenceLongBookDao.save(SequenceLongBook("Test"))

    book.shouldBeInstanceOf<AuditedSequenceIdEntity<Long>>()
  }

  @Test
  fun `should generate valid sequence Long IDs`() {
    val book1 = sequenceLongBookDao.save(SequenceLongBook("Book 1"))
    val book2 = sequenceLongBookDao.save(SequenceLongBook("Book 2"))

    book1.id.shouldBeInstanceOf<Long>()
    book2.id.shouldBeInstanceOf<Long>()
    book1.id shouldNotBe book2.id
  }
}
