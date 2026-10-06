package zygarde.test

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.jpa.domain.Specification
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.support.TransactionSynchronizationManager
import zygarde.test.dao.TestBookDao
import zygarde.test.entity.Book

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = [ZygardeJpaTestApplication::class])
@ActiveProfiles("test")
@DirtiesContext
class ZygardeJpaRepositoryTransactionTest {
  @Autowired
  lateinit var bookDao: TestBookDao

  @Test
  fun `delete by specification without outer transaction should run in writable transaction`() {
    bookDao.saveAll(listOf(Book(), Book()))
    var txActive: Boolean? = null
    var txReadOnly: Boolean? = null

    val deleted = bookDao.delete(
      Specification<Book> { _, _, cb ->
        txActive = TransactionSynchronizationManager.isActualTransactionActive()
        txReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly()
        cb.conjunction()
      }
    )

    txActive shouldBe true
    txReadOnly shouldBe false
    deleted shouldBe 2L
    bookDao.count() shouldBe 0L
  }
}
