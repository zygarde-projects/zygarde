package example

import zygarde.codegen.ZyModel
import zygarde.codegen.meta.ZyModelMeta
import zygarde.data.jpa.entity.AutoIntIdEntity
import javax.persistence.Entity
import javax.persistence.MappedSuperclass

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
) : AbstractNote()

@ZyModelMeta
@MappedSuperclass
abstract class AbstractNote(
  var title: String = "",
) : AutoIntIdEntity()
