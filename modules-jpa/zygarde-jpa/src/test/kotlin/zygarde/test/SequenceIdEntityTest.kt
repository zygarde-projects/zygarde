package zygarde.test

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.util.ReflectionTestUtils
import zygarde.test.dao.SequenceAuthorDao
import zygarde.test.entity.SequenceAuthor

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = [ZygardeJpaTestApplication::class])
@ActiveProfiles("test", "test-oracle")
@DirtiesContext
class SequenceIdEntityTest {
  @Autowired
  lateinit var sequenceAuthorDao: SequenceAuthorDao

  @Test
  fun `should able to save and increase id`() {
    sequenceAuthorDao.saveAndFlush(
      SequenceAuthor("foo")
    ).id shouldBe 1
  }

  @Test
  fun `equals should compare non-null ids for same entity type`() {
    val first = SequenceAuthor("first")
    val sameId = SequenceAuthor("same")
    val differentId = SequenceAuthor("different")

    ReflectionTestUtils.setField(first, "id", 7)
    ReflectionTestUtils.setField(sameId, "id", 7)
    ReflectionTestUtils.setField(differentId, "id", 8)

    (first == sameId) shouldBe true
    (first == differentId) shouldBe false
    (first == SequenceAuthor("unsaved")) shouldBe false
    (first.equals(null)) shouldBe false
    first.hashCode() shouldBe 7.hashCode()
  }
}
