package example

import zygarde.codegen.ZyModel
import zygarde.core.annotation.Comment
import zygarde.data.jpa.entity.AutoIntIdEntity
import zygarde.jpa.converter.StringListToJsonStringConverter
import javax.persistence.Convert
import javax.persistence.Entity
import javax.persistence.MappedSuperclass

@Entity
@ZyModel
class Todo(
  var description: String = "",
  var checkTimes: Int = 0,
) : AutoIntIdEntity()

@Entity
@ZyModel
class Note : AbstractNote()

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
