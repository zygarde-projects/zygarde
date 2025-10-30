package zygarde.data.jpa.audit

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder

class AuditorProviderTest {

  class TestAuditedUser(private val username: String) : AuditedUserVo {
    override fun auditInfo(): String = username
  }

  @Test
  fun `should return current auditor from security context`() {
    // given
    val auditorProvider = AuditorProvider()
    val user = TestAuditedUser("testuser")

    val authentication = mockk<Authentication>()
    every { authentication.isAuthenticated } returns true
    every { authentication.details } returns user

    val securityContext = mockk<SecurityContext>()
    every { securityContext.authentication } returns authentication

    SecurityContextHolder.setContext(securityContext)

    // when
    val result = auditorProvider.getCurrentAuditor()

    // then
    result.isPresent shouldBe true
    result.get() shouldBe "testuser"

    // cleanup
    SecurityContextHolder.clearContext()
  }

  @Test
  fun `should return empty string when no authentication`() {
    // given
    val auditorProvider = AuditorProvider()
    SecurityContextHolder.clearContext()

    // when
    val result = auditorProvider.getCurrentAuditor()

    // then
    result.isPresent shouldBe true
    result.get() shouldBe ""
  }

  @Test
  fun `should return empty string when authentication is not authenticated`() {
    // given
    val auditorProvider = AuditorProvider()

    val authentication = mockk<Authentication>()
    every { authentication.isAuthenticated } returns false

    val securityContext = mockk<SecurityContext>()
    every { securityContext.authentication } returns authentication

    SecurityContextHolder.setContext(securityContext)

    // when
    val result = auditorProvider.getCurrentAuditor()

    // then
    result.isPresent shouldBe true
    result.get() shouldBe ""

    // cleanup
    SecurityContextHolder.clearContext()
  }

  @Test
  fun `should return empty string when details is not AuditedUserVo`() {
    // given
    val auditorProvider = AuditorProvider()

    val authentication = mockk<Authentication>()
    every { authentication.isAuthenticated } returns true
    every { authentication.details } returns "not an AuditedUserVo"

    val securityContext = mockk<SecurityContext>()
    every { securityContext.authentication } returns authentication

    SecurityContextHolder.setContext(securityContext)

    // when
    val result = auditorProvider.getCurrentAuditor()

    // then
    result.isPresent shouldBe true
    result.get() shouldBe ""

    // cleanup
    SecurityContextHolder.clearContext()
  }
}
