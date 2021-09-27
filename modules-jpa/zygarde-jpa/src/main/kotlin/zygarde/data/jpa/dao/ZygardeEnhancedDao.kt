package zygarde.data.jpa.dao

import org.springframework.data.jpa.domain.Specification
import org.springframework.data.repository.NoRepositoryBean

@NoRepositoryBean
interface ZygardeEnhancedDao<T, ID> : BaseDao<T, ID> {
  fun delete(spec: Specification<T>)
}
