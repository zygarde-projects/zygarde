package zygarde.data.jpa

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import zygarde.data.jpa.dao.SequenceAuthorDao
import zygarde.data.jpa.entity.SequenceAuthor

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
}
