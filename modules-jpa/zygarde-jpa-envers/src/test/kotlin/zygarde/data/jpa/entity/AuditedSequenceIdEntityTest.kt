package zygarde.data.jpa.entity

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import zygarde.test.ZygardeJpaTestApplication
import zygarde.test.dao.SequenceIntAuthorDao
import zygarde.test.dao.SequenceLongBookDao
import zygarde.test.entity.SequenceIntAuthor
import zygarde.test.entity.SequenceLongBook
import java.time.LocalDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = [ZygardeJpaTestApplication::class])
@ActiveProfiles("test", "test-oracle")
@DirtiesContext
class AuditedSequenceIdEntityTest {
  @Autowired
  lateinit var sequenceIntAuthorDao: SequenceIntAuthorDao

  @Autowired
  lateinit var sequenceLongBookDao: SequenceLongBookDao

  @Test
  fun `should populate createdAt on entity creation with sequence ID`() {
    val before = LocalDateTime.now()
    val author = sequenceIntAuthorDao.save(SequenceIntAuthor("Test Author"))
    val after = LocalDateTime.now()

    author.createdAt shouldNotBe null
    author.createdAt.shouldNotBeNull()
    (author.createdAt.isAfter(before) || author.createdAt.isEqual(before)) shouldBe true
    (author.createdAt.isBefore(after) || author.createdAt.isEqual(after)) shouldBe true
  }

  @Test
  fun `should populate updatedAt on sequence entity update`() {
    val author = sequenceIntAuthorDao.save(SequenceIntAuthor("Original Name"))
    val createdAt = author.createdAt
    val initialUpdatedAt = author.updatedAt

    Thread.sleep(100)

    author.name = "Updated Name"
    val updated = sequenceIntAuthorDao.saveAndFlush(author)

    updated.createdAt shouldBe createdAt
    updated.updatedAt.shouldNotBeNull()
    if (initialUpdatedAt != null) {
      updated.updatedAt!! shouldNotBe initialUpdatedAt
    }
  }

  @Test
  fun `should generate audit container key for SequenceIntIdEntity`() {
    val author = sequenceIntAuthorDao.save(SequenceIntAuthor("Test"))
    val key = author.auditContainerKey()

    key shouldNotBe null
    key.contains(author.id.toString()) shouldBe true
  }

  @Test
  fun `should generate audit container key for SequenceLongIdEntity`() {
    val book = sequenceLongBookDao.save(SequenceLongBook("Test Book"))
    val key = book.auditContainerKey()

    key shouldNotBe null
    key.contains(book.id.toString()) shouldBe true
  }

  @Test
  fun `should preserve audit fields across multiple updates`() {
    val author = sequenceIntAuthorDao.save(SequenceIntAuthor("V1"))
    val originalCreatedAt = author.createdAt

    Thread.sleep(10)
    author.name = "V2"
    sequenceIntAuthorDao.saveAndFlush(author)

    Thread.sleep(10)
    author.name = "V3"
    val final = sequenceIntAuthorDao.saveAndFlush(author)

    final.createdAt shouldBe originalCreatedAt
  }

  @Test
  fun `should have audit fields initialized`() {
    val author = sequenceIntAuthorDao.save(SequenceIntAuthor("Test"))

    author.createdAt.shouldNotBeNull()
    // updatedAt is null until first update
    // createdBy/updatedBy populated by AuditorProvider if security context available
  }
}
