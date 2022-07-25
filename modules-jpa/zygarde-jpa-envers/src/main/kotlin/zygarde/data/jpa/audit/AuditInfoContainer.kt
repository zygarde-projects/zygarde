package zygarde.data.jpa.audit

import java.io.Serializable
import java.time.LocalDateTime

interface AuditInfoContainer : Serializable {
  fun auditContainerKey(): String
  var createdAt: LocalDateTime
  var updatedAt: LocalDateTime?
  var createdBy: String?
  var updatedBy: String?
}
