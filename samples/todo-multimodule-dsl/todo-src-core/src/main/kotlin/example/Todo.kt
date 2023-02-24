package example

import zygarde.codegen.ZyModel
import zygarde.core.annotation.Comment
import zygarde.codegen.meta.ZyModelMeta
import zygarde.data.jpa.entity.AutoIntIdEntity
import zygarde.jpa.converter.StringListToJsonStringConverter
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.MappedSuperclass

@Entity
@ZyModel
@ZyModelMeta
class Todo(
  var description: String = "",
  var checkTimes: Int = 0,
) : AutoIntIdEntity()

@Entity
@ZyModel
@ZyModelMeta
class Note : AbstractNote()

@ZyModelMeta
@MappedSuperclass
abstract class AbstractNote(
  var title: String = "",
) : AutoIntIdEntity()

@Entity
class Mark(
  @Comment("x")
  var x: Int,
  @Comment("y")
  var y: Int,
  @Convert(converter = StringListToJsonStringConverter::class)
  var comments: Collection<String>,
) : AutoIntIdEntity()
