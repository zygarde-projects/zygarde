package zygarde.data.jpa.entity

import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import zygarde.test.ZygardeJpaTestApplication
import zygarde.test.dao.AutoLongBookDao
import zygarde.test.entity.AutoLongBook

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = [ZygardeJpaTestApplication::class])
@ActiveProfiles("test")
@DirtiesContext
class AuditedAutoLongIdEntityTest {
  @Autowired
  lateinit var autoLongBookDao: AutoLongBookDao

  @Test
  fun `should implement AutoIdGetter interface with Long type`() {
    val book = autoLongBookDao.save(AutoLongBook("Test"))

    book.shouldBeInstanceOf<AutoIdGetter<Long>>()
    book.id.shouldBeInstanceOf<Long>()
  }

  @Test
  fun `should properly inherit from AuditedAutoIdEntity with Long type`() {
    val book = autoLongBookDao.save(AutoLongBook("Test"))

    book.shouldBeInstanceOf<AuditedAutoIdEntity<Long>>()
  }

  @Test
  fun `should generate valid Long IDs`() {
    val book1 = autoLongBookDao.save(AutoLongBook("Book 1"))
    val book2 = autoLongBookDao.save(AutoLongBook("Book 2"))

    book1.id.shouldBeInstanceOf<Long>()
    book2.id.shouldBeInstanceOf<Long>()
    book1.id shouldNotBe book2.id
  }
}
