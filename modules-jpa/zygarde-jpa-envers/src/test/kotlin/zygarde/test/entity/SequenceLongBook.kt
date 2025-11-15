package zygarde.test.entity

import org.hibernate.envers.Audited
import zygarde.data.jpa.entity.AuditedSequenceLongIdEntity
import jakarta.persistence.Entity

@Audited
@Entity
open class SequenceLongBook(
  var name: String = ""
) : AuditedSequenceLongIdEntity()
