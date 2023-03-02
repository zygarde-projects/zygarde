package zygarde.data.jpa.search

import zygarde.data.jpa.search.impl.EnhancedSearchImpl
import javax.persistence.criteria.Predicate

inline fun <reified JoinTarget> EnhancedSearch<*>.crossJoin(joinSearchContent: (joinSearch: EnhancedSearch<JoinTarget>) -> Unit) {
  if (this is EnhancedSearchImpl) {
    val predicatesForJoin = mutableListOf<Predicate>()
    val joinRoot = query.from(JoinTarget::class.java)
    val enhancedSearchImplForJoinTarget = EnhancedSearchImpl<JoinTarget>(
      predicatesForJoin,
      joinRoot,
      query,
      cb
    )
    joinSearchContent.invoke(enhancedSearchImplForJoinTarget)

    predicates.add(
      cb.and(*predicatesForJoin.toTypedArray())
    )
  }
}
