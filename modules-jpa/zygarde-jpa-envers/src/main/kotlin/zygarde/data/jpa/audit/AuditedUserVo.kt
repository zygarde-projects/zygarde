package zygarde.data.jpa.audit

import java.io.Serializable

interface AuditedUserVo : Serializable {
  fun auditInfo(): String
}
