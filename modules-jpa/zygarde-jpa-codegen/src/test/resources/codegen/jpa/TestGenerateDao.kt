package codegen.jpa

import com.fasterxml.jackson.annotation.JsonProperty
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
  var id: Long,
  @field:JsonProperty("book_price")
  var price: Int = 0,
  var amount: String? = null,
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

/**
 * Regression fixture for KotlinPoet line-wrapping in generated DAO extensions.
 * The long class name forces the generated `search(sorts, searchContent)` body
 * past KotlinPoet's default wrap column so any wrap between `.let` and `{`
 * would break compilation of the generated file.
 */
@ZyModel
@Entity
class VeryLongEntityNameForceWrapRegressionBookForDaoExtensions(
  @Id
  var id: Long
)
