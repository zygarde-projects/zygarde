package codegen.jpa

import zygarde.codegen.NullEquivalent
import zygarde.codegen.ScopeMarker
import zygarde.codegen.ScopeOp
import zygarde.codegen.ScopeOperator
import zygarde.codegen.ZyModel
import zygarde.data.jpa.search.ScopeFilter
import jakarta.persistence.Entity
import jakarta.persistence.Id

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

@ScopeMarker
interface AdvancedScopedEntity {
  @get:NullEquivalent("ARCHIVED")
  @get:ScopeOp(ScopeOperator.NOT_IN)
  val status: String?

  @get:ScopeOp(ScopeOperator.IS_NULL)
  val deletedAt: String?

  @get:ScopeOp(ScopeOperator.IS_NOT_NULL)
  val linkedId: Long?
}

@ScopeMarker
interface FilterValueScoped {
  val filter: ScopeFilter<String>
}

@ScopeMarker
interface CollectionValueScoped {
  val values: Collection<String>
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

@ZyModel
@Entity
class AdvancedScopedOrder(
  @Id
  var id: Long,
  override val status: String? = null,
  override val deletedAt: String? = null,
  override val linkedId: Long? = null,
) : AdvancedScopedEntity

@ZyModel
@Entity
class FilterValueEntity(
  @Id
  var id: Long,
  override val filter: ScopeFilter<String> = ScopeFilter.All,
) : FilterValueScoped

@ZyModel
@Entity
class CollectionValueEntity(
  @Id
  var id: Long,
  override val values: Collection<String> = emptyList(),
) : CollectionValueScoped

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

/**
 * Regression fixture: long class name forces KotlinPoet to wrap the generated
 * scoped sorted-search body. A wrap between `.let` and `{` would break the
 * generated file compilation.
 */
@ZyModel
@Entity
class VeryLongEntityNameScopedRegressionEntityForDaoExtensions(
  @Id
  var id: Long,
  override val regionId: Long = 0,
) : RegionScoped
