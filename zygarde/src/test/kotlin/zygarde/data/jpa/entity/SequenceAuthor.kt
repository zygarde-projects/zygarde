package zygarde.data.jpa.entity

import javax.persistence.Entity

@Entity
open class SequenceAuthor(
  var name: String = ""
) : SequenceIntIdEntity()
