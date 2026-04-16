package codegen.jpa

import zygarde.codegen.NullEquivalent
import zygarde.codegen.ScopeMarker
import zygarde.codegen.ZyModel
import javax.persistence.Entity
import javax.persistence.Id

@ScopeMarker
interface PlatformScopedEntity {
  @get:NullEquivalent("HOTCAKE")
  val platformSource: String?
}

@ScopeMarker
interface TenantScopedEntity {
  val tenantId: String
}

@ScopeMarker
interface FeatureToggleScoped {
  val enabled: Boolean
}

@ZyModel
@Entity
class ScopedOrder(
  @Id
  var id: Long,
  override val platformSource: String? = null,
) : PlatformScopedEntity

@ZyModel
@Entity
class MultiScopedOrder(
  @Id
  var id: Long,
  override val platformSource: String? = null,
  override val tenantId: String = "",
) : PlatformScopedEntity, TenantScopedEntity

@ScopeMarker
interface RegionScoped {
  val regionId: Long
}

@ZyModel
@Entity
class ToggleableItem(
  @Id
  var id: Long,
  override val enabled: Boolean = true,
) : FeatureToggleScoped

@ZyModel
@Entity
class RegionalProduct(
  @Id
  var id: Long,
  override val regionId: Long = 0,
) : RegionScoped

@ScopeMarker
interface OwnerScoped {
  val ownerId: Long
}

interface AuditableOwned : OwnerScoped

@ZyModel
@Entity
class OwnedDocument(
  @Id
  var id: Long,
  override val ownerId: Long = 0,
) : AuditableOwned

@ScopeMarker
interface EmptyScopeMarker

@ZyModel
@Entity
class UnscopedByEmptyMarker(
  @Id
  var id: Long,
) : EmptyScopeMarker
