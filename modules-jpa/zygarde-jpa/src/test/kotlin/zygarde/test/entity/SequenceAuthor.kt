package zygarde.test.entity

import zygarde.data.jpa.entity.SequenceIntIdEntity
import javax.persistence.Entity

@Entity
open class SequenceAuthor(
  var name: String = ""
) : SequenceIntIdEntity()
