package example

import zygarde.codegen.ZyModel
import zygarde.codegen.meta.ZyModelMeta
import zygarde.data.jpa.entity.AutoIntIdEntity
import javax.persistence.Entity

@Entity
@ZyModel
@ZyModelMeta
class Todo(
  var description: String = "",
) : AutoIntIdEntity()

@Entity
@ZyModel
@ZyModelMeta
class Note(
  var title: String = "",
) : AutoIntIdEntity()
