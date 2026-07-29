package zygarde.data.jpa.search

/** A scoped-query filter that distinguishes an explicit bypass from a value set. */
sealed interface ScopeFilter<out T> {
  /** Do not add a predicate for this scope field. */
  data object All : ScopeFilter<Nothing>

  /** Match this value set. An empty set intentionally matches nothing. */
  data class Of<T>(val values: Collection<T>) : ScopeFilter<T>
}
