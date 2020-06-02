package zygarde.data.jpa.entity

import javax.persistence.Entity

@Entity
open class AuthorGroup(
  var name: String = ""
) : AutoIntIdEntity()
