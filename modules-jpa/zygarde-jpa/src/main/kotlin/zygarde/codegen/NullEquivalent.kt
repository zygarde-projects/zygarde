package zygarde.codegen

/**
 * Marks a scope interface property to generate OR IS NULL logic
 * when the specified value is included in the scope collection.
 *
 * ```
 * @ScopeMarker
 * interface PlatformScopedEntity {
 *   @get:NullEquivalent("HOTCAKE")
 *   val platformSource: String?
 * }
 * ```
 *
 * When the scope contains "HOTCAKE", the generated predicate becomes:
 * `platformSource IN (...) OR platformSource IS NULL`
 *
 * **Constraint:** the generated sentinel check compares scope entries via
 * `it.toString() == "<value>"`, so the scope field type must be either `String`
 * or an `Enum` whose default `toString()` equals `name()`. Types that override
 * `toString()` (e.g. value classes, data classes, custom formatters) will
 * silently miss the sentinel and skip the `OR IS NULL` branch.
 *
 * @param value the sentinel value that triggers OR IS NULL behavior
 */
@Target(AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.BINARY)
annotation class NullEquivalent(val value: String)
