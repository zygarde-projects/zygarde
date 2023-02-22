package zygarde.test.entity

import org.hibernate.envers.Audited
import zygarde.data.jpa.entity.AuditedSequenceIntIdEntity
import jakarta.persistence.Entity

@Audited
@Entity
open class AutoLongBook(
  var name: String = ""
) : AuditedSequenceIntIdEntity()
