package zygarde.test.dao

import org.springframework.data.jpa.repository.JpaRepository
import zygarde.test.entity.SequenceLongBook

interface SequenceLongBookDao : JpaRepository<SequenceLongBook, Long>
