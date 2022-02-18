package zygarde.test.entity

import org.hibernate.envers.Audited
import zygarde.data.jpa.entity.AuditedSequenceIntIdEntity
import javax.persistence.Entity

@Audited
@Entity
open class SequenceLongBook(
  var name: String = ""
) : AuditedSequenceIntIdEntity()
