package zygarde.samples.todo

import zygarde.codegen.NullEquivalent
import zygarde.codegen.ScopeMarker
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
