package zygarde.data.jpa.entity

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class AuditedEntityTest {

  class TestEntity : AuditedEntity() {
    override fun auditContainerKey(): String = "test"
  }

  @Test
  fun `should initialize with default audit values`() {
    // given & when
    val entity = TestEntity()

    // then
    entity.createdAt shouldNotBe null
    entity.updatedAt shouldBe null
    entity.createdBy shouldBe null
    entity.updatedBy shouldBe null
  }

  @Test
  fun `should set audit information`() {
    // given
    val entity = TestEntity()
    val updateTime = LocalDateTime.now().plusMinutes(10)

    // when
    entity.createdBy = "creator"
    entity.updatedAt = updateTime
    entity.updatedBy = "updater"

    // then
    entity.createdBy shouldBe "creator"
    entity.updatedAt shouldBe updateTime
    entity.updatedBy shouldBe "updater"
  }

  @Test
  fun `should track creation time`() {
    // given
    val before = LocalDateTime.now().minusSeconds(1)

    // when
    val entity = TestEntity()

    // then
    val after = LocalDateTime.now().plusSeconds(1)
    entity.createdAt.isAfter(before) shouldBe true
    entity.createdAt.isBefore(after) shouldBe true
  }
}
