package zygarde.data.jpa.dao

import org.springframework.data.jpa.repository.JpaRepository
import zygarde.data.jpa.entity.SequenceAuthor

interface SequenceAuthorDao : JpaRepository<SequenceAuthor, Int>
