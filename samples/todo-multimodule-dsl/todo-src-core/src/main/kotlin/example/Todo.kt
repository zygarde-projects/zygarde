package example

import zygarde.codegen.ZyModel
import zygarde.core.annotation.Comment
import zygarde.data.jpa.entity.AutoIntIdEntity
import zygarde.jpa.converter.StringListToJsonStringConverter
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MappedSuperclass
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.NotEmpty

@Entity
@ZyModel
class Todo(
  var description: String = "",
  @Column(name = "file_id")
  var fileId: String? = "todo-file",
  var checkTimes: Int = 0,
) : AutoIntIdEntity()

@Entity
@ZyModel
class Note : AbstractNote()

@Entity
@ZyModel
open class GraphQlAuthor(
  open var name: String = "",
) : AutoIntIdEntity()

@Entity
@ZyModel
open class GraphQlBook(
  open var title: String = "",
  @Column(name = "author_id")
  open var authorId: Int? = null,
  @ManyToOne(targetEntity = GraphQlAuthor::class, fetch = FetchType.LAZY)
  @JoinColumn(name = "author_id", insertable = false, updatable = false)
  open var author: GraphQlAuthor? = null,
) : AutoIntIdEntity()

@MappedSuperclass
abstract class AbstractNote(
  var title: String = "",
) : AutoIntIdEntity()

@Entity
class Mark(
  @field:NotEmpty
  @field:DecimalMax("100")
  @Comment("x")
  var x: Int,
  @Comment("y")
  var y: Int,
  @Convert(converter = StringListToJsonStringConverter::class)
  var comments: Collection<String>,
) : AutoIntIdEntity()
