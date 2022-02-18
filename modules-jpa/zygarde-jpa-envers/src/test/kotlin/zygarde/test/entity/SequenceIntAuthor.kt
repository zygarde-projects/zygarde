package zygarde.test.entity

import org.hibernate.envers.Audited
import zygarde.data.jpa.entity.AuditedSequenceIntIdEntity
import javax.persistence.Entity

@Audited
@Entity
open class SequenceIntAuthor(
  var name: String = ""
) : AuditedSequenceIntIdEntity()
