package zygarde.data.jpa.dao

import org.springframework.data.jpa.domain.Specification
import org.springframework.data.repository.NoRepositoryBean
import zygarde.data.jpa.search.EnhancedSearch
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

@NoRepositoryBean
interface ZygardeEnhancedDao<T, ID> : BaseDao<T, ID> {
  fun delete(spec: Specification<T>)

  fun <P> selectOne(
    p: KProperty1<T, P>,
    searchContent: EnhancedSearch<T>.() -> Unit
  ): P

  fun <P> select(
    p: KProperty1<T, P>,
    searchContent: EnhancedSearch<T>.() -> Unit
  ): List<P>

  fun <P : Any> select(projection: KClass<P>, searchContent: EnhancedSearch<T>.() -> Unit): List<P>
}
