package zygarde.test.entity

import zygarde.data.jpa.entity.AutoIntIdEntity
import jakarta.persistence.Entity

@Entity
open class AuthorGroup(
  var name: String = ""
) : AutoIntIdEntity()
