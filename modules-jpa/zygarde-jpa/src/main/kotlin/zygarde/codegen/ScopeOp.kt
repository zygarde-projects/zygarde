package zygarde.codegen

/**
 * Selects the predicate generated for a scoped entity property.
 *
 * [IS_NULL] and [IS_NOT_NULL] generate a Boolean scope field. `true` applies
 * the null check; `false` (or `null` for an optional scope field) skips it.
 * [NullEquivalent] only affects [IN] and [NOT_IN]. For [NOT_IN], excluding the
 * sentinel also excludes SQL NULL; otherwise SQL NULL remains included.
 */
@Target(AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.BINARY)
annotation class ScopeOp(val value: ScopeOperator = ScopeOperator.IN)

enum class ScopeOperator {
  IN,
  NOT_IN,
  IS_NULL,
  IS_NOT_NULL,
}
