package zygarde.data.jpa.audit

import java.time.LocalDateTime

interface AuditInfoContainer {
  fun auditContainerKey(): String
  var createdAt: LocalDateTime
  var updatedAt: LocalDateTime?
  var createdBy: String?
  var updatedBy: String?
}
