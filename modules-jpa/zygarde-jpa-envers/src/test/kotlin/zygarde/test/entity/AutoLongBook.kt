package zygarde.test.entity

import org.hibernate.envers.Audited
import zygarde.data.jpa.entity.AuditedAutoLongIdEntity
import javax.persistence.Entity

@Audited
@Entity
open class AutoLongBook(
  var name: String = ""
) : AuditedAutoLongIdEntity()
