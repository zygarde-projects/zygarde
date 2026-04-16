package zygarde.data.jpa.search

import org.springframework.data.jpa.domain.Specification
import zygarde.data.jpa.search.impl.EnhancedSearchImpl
import javax.persistence.criteria.Predicate

object SearchSpecBuilder {
  fun <T> buildSpec(searchContent: EnhancedSearch<T>.() -> Unit): Specification<T> {
    return Specification<T> { root, query, cb ->
      val predicates = mutableListOf<Predicate>()
      val enhancedSearchImpl = EnhancedSearchImpl(predicates, root, query, cb)
      searchContent.invoke(enhancedSearchImpl)
      if (enhancedSearchImpl.orders.isNotEmpty()) {
        query.orderBy(enhancedSearchImpl.orders)
      }
      cb.and(*predicates.toTypedArray())
    }
  }
}
