package zygarde.test.dao

import org.springframework.data.jpa.repository.JpaRepository
import zygarde.test.entity.SequenceIntAuthor

interface SequenceIntAuthorDao : JpaRepository<SequenceIntAuthor, Int>
