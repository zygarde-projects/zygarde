package zygarde.test.dao

import org.springframework.data.jpa.repository.JpaRepository
import zygarde.test.entity.AutoIntAuthor

interface AutoIntAuthorDao : JpaRepository<AutoIntAuthor, Int>
