package codegen.jpa

import zygarde.codegen.ZyModel
import zygarde.data.jpa.entity.AuditedAutoIntIdEntity
import zygarde.data.jpa.entity.AuditedSequenceIntIdEntity
import zygarde.data.jpa.entity.AutoIntIdEntity
import zygarde.data.jpa.entity.AutoLongIdEntity
import zygarde.data.jpa.entity.SequenceIntIdEntity
import java.io.Serializable
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass

@ZyModel
@Entity
class SimpleBook(
  @Id
  var id: Long
)

@ZyModel
@Entity
class AutoIntIdBook : AutoIntIdEntity()

@ZyModel
@Entity
class AutoLongIdBook(
  @Id
  override val id: Long?,
) : AutoLongIdEntity()

@ZyModel
@Entity
class AuditedAutoIntIdBook : AuditedAutoIntIdEntity()

@ZyModel
@Entity
class SequenceAutoIntIdBook : AuditedSequenceIntIdEntity()

data class BookId(val isbn: String, val country: String) : Serializable

@ZyModel
@IdClass(BookId::class)
@Entity
class IdClassBook(
  val isbn: String,
  val country: String
)

@Entity
@ZyModel
class SequenceBook(
  var name: String = ""
) : SequenceIntIdEntity()
