package zygarde.data.jpa.audit

import org.springframework.data.domain.AuditorAware
import org.springframework.security.core.context.SecurityContextHolder
import zygarde.core.log.Loggable
import java.util.Optional

class AuditorProvider : AuditorAware<String>, Loggable {
  override fun getCurrentAuditor(): Optional<String> {
    val auth = SecurityContextHolder.getContext()?.authentication
      ?.takeIf { it.isAuthenticated && it.details is AuditedUserVo }
      ?.let { it.details as AuditedUserVo? }
      ?.auditInfo()
      .orEmpty()
    return Optional.of((auth).also { LOGGER.trace("audit: $it") })
  }
}
