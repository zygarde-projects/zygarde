package zygarde.test.dao

import org.springframework.data.jpa.repository.JpaRepository
import zygarde.test.entity.SequenceAuthor

interface SequenceAuthorDao : JpaRepository<SequenceAuthor, Int>
