package zygarde.data.jpa.audit

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class AuditedUserVoTest {

  class TestUser(private val username: String) : AuditedUserVo {
    override fun auditInfo(): String = username
  }

  @Test
  fun `should return audit info`() {
    // given
    val user = TestUser("john.doe")

    // when
    val auditInfo = user.auditInfo()

    // then
    auditInfo shouldBe "john.doe"
  }

  @Test
  fun `should support different audit info formats`() {
    // given
    class EmailAuditUser(private val email: String) : AuditedUserVo {
      override fun auditInfo(): String = email
    }

    val user = EmailAuditUser("user@example.com")

    // when
    val auditInfo = user.auditInfo()

    // then
    auditInfo shouldBe "user@example.com"
  }
}
