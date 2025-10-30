package zygarde.test.entity

import org.hibernate.envers.Audited
import zygarde.data.jpa.entity.AuditedAutoIntIdEntity
import javax.persistence.Entity

@Audited
@Entity
open class AutoIntAuthor(
  var name: String = ""
) : AuditedAutoIntIdEntity()
