package zygarde.samples.todo

import zygarde.codegen.NullEquivalent
import zygarde.codegen.ScopeMarker
import zygarde.codegen.ScopeOp
import zygarde.codegen.ScopeOperator
import zygarde.codegen.ZyModel
import zygarde.data.jpa.entity.AutoLongIdEntity
import jakarta.persistence.Entity

@ScopeMarker
interface TenantScoped {
  val tenantId: String
}

@ScopeMarker
interface VisibilityScoped {
  @get:NullEquivalent("PUBLIC")
  val visibility: String?
}

@ZyModel
@Entity
class Note(
  var title: String = "",
  override var tenantId: String = "",
  override var visibility: String? = null,
) : AutoLongIdEntity(), TenantScoped, VisibilityScoped

enum class ScopedTaskStatus {
  ACTIVE,
  CANCELLED,
  ARCHIVED,
}

@ScopeMarker
interface ScopedTaskFilters {
  @get:NullEquivalent("ARCHIVED")
  @get:ScopeOp(ScopeOperator.NOT_IN)
  val status: ScopedTaskStatus?

  @get:NullEquivalent("ARCHIVED-CODE")
  @get:ScopeOp(ScopeOperator.NOT_IN)
  val excludedCode: String?

  @get:ScopeOp(ScopeOperator.IS_NULL)
  val deletedAt: String?

  @get:ScopeOp(ScopeOperator.IS_NOT_NULL)
  val linkedId: Long?
}

@ZyModel
@Entity
class ScopedTask(
  var name: String = "",
  override var status: ScopedTaskStatus? = null,
  override var excludedCode: String? = null,
  override var deletedAt: String? = null,
  override var linkedId: Long? = null,
) : AutoLongIdEntity(), ScopedTaskFilters
