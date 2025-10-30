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
import zygarde.test.dao.AutoIntAuthorDao
import zygarde.test.dao.AutoLongBookDao
import zygarde.test.entity.AutoIntAuthor
import zygarde.test.entity.AutoLongBook
import java.time.LocalDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = [ZygardeJpaTestApplication::class])
@ActiveProfiles("test")
@DirtiesContext
class AuditedAutoIdEntityTest {

  @Autowired
  lateinit var autoIntAuthorDao: AutoIntAuthorDao

  @Autowired
  lateinit var autoLongBookDao: AutoLongBookDao

  @Test
  fun `should populate createdAt on entity creation`() {
    val before = LocalDateTime.now()
    val author = autoIntAuthorDao.save(AutoIntAuthor("Test Author"))
    val after = LocalDateTime.now()

    author.createdAt shouldNotBe null
    author.createdAt.shouldNotBeNull()
    (author.createdAt.isAfter(before) || author.createdAt.isEqual(before)) shouldBe true
    (author.createdAt.isBefore(after) || author.createdAt.isEqual(after)) shouldBe true
  }

  @Test
  fun `should populate updatedAt on entity update`() {
    val author = autoIntAuthorDao.save(AutoIntAuthor("Original Name"))
    val createdAt = author.createdAt
    val initialUpdatedAt = author.updatedAt

    Thread.sleep(100) // Ensure time difference

    author.name = "Updated Name"
    val updated = autoIntAuthorDao.saveAndFlush(author)

    updated.createdAt shouldBe createdAt
    updated.updatedAt.shouldNotBeNull()
    if (initialUpdatedAt != null) {
      updated.updatedAt!! shouldNotBe initialUpdatedAt
    }
  }

  @Test
  fun `should generate audit container key for AutoIntIdEntity`() {
    val author = autoIntAuthorDao.save(AutoIntAuthor("Test"))
    val key = author.auditContainerKey()

    key shouldNotBe null
    key.contains(author.id.toString()) shouldBe true
  }

  @Test
  fun `should generate audit container key for AutoLongIdEntity`() {
    val book = autoLongBookDao.save(AutoLongBook("Test Book"))
    val key = book.auditContainerKey()

    key shouldNotBe null
    key.contains(book.id.toString()) shouldBe true
  }

  @Test
  fun `should have audit fields initialized`() {
    val author = autoIntAuthorDao.save(AutoIntAuthor("Test"))

    author.createdAt.shouldNotBeNull()
    // updatedAt is null until first update
    // createdBy/updatedBy populated by AuditorProvider if security context available
  }

  @Test
  fun `should preserve createdAt across multiple updates`() {
    val author = autoIntAuthorDao.save(AutoIntAuthor("V1"))
    val originalCreatedAt = author.createdAt

    Thread.sleep(10)
    author.name = "V2"
    autoIntAuthorDao.saveAndFlush(author)

    Thread.sleep(10)
    author.name = "V3"
    val final = autoIntAuthorDao.saveAndFlush(author)

    final.createdAt shouldBe originalCreatedAt
  }
}
