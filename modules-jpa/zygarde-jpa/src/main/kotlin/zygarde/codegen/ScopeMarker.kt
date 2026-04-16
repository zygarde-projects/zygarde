package zygarde.codegen

/**
 * Marks an interface as a scope marker for code generation.
 *
 * When an entity annotated with [ZyModel] implements an interface annotated with [ScopeMarker],
 * the code generator:
 * 1. Generates a `{Entity}Scope` data class from all scope interface properties
 * 2. Generates scoped extension functions (search, remove, etc.) that require a scope parameter
 * 3. Does NOT generate unscoped overloads — compiler enforces scoping
 *
 * ```
 * @ScopeMarker
 * interface PlatformScopedEntity {
 *   val platformSource: String?
 * }
 *
 * @ZyModel
 * @Entity
 * class Order(
 *   override val platformSource: String? = null,
 * ) : PlatformScopedEntity
 *
 * // Generated: OrderScope data class + scoped extension functions
 * // orderDao.search(OrderScope(platformSource = "HOTCAKE")) { ... }
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class ScopeMarker
