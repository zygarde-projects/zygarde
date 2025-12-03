package zygarde.data.jpa.audit

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class AuditInfoContainerTest {
  class TestAuditContainer : AuditInfoContainer {
    override fun auditContainerKey(): String = "test-key"

    override var createdAt: LocalDateTime = LocalDateTime.now()
    override var updatedAt: LocalDateTime? = null
    override var createdBy: String? = null
    override var updatedBy: String? = null
  }

  @Test
  fun `should implement audit container with key`() {
    // given & when
    val container = TestAuditContainer()

    // then
    container.auditContainerKey() shouldBe "test-key"
  }

  @Test
  fun `should track creation timestamp`() {
    // given
    val container = TestAuditContainer()
    val before = LocalDateTime.now().minusSeconds(1)
    val after = LocalDateTime.now().plusSeconds(1)

    // then
    container.createdAt shouldNotBe null
    container.createdAt.isAfter(before) shouldBe true
    container.createdAt.isBefore(after) shouldBe true
  }

  @Test
  fun `should track update timestamp`() {
    // given
    val container = TestAuditContainer()
    val updateTime = LocalDateTime.now()

    // when
    container.updatedAt = updateTime

    // then
    container.updatedAt shouldBe updateTime
  }

  @Test
  fun `should track created by user`() {
    // given
    val container = TestAuditContainer()

    // when
    container.createdBy = "john.doe"

    // then
    container.createdBy shouldBe "john.doe"
  }

  @Test
  fun `should track updated by user`() {
    // given
    val container = TestAuditContainer()

    // when
    container.updatedBy = "jane.smith"

    // then
    container.updatedBy shouldBe "jane.smith"
  }

  @Test
  fun `should support full audit lifecycle`() {
    // given
    val container = TestAuditContainer()
    val createdTime = LocalDateTime.now()
    val updatedTime = LocalDateTime.now().plusMinutes(5)

    // when - simulate entity creation
    container.createdAt = createdTime
    container.createdBy = "creator"

    // when - simulate entity update
    container.updatedAt = updatedTime
    container.updatedBy = "updater"

    // then
    container.createdAt shouldBe createdTime
    container.createdBy shouldBe "creator"
    container.updatedAt shouldBe updatedTime
    container.updatedBy shouldBe "updater"
  }
}
