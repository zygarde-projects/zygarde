package zygarde.test.dao

import org.springframework.data.jpa.repository.JpaRepository
import zygarde.test.entity.AutoLongBook

interface AutoLongBookDao : JpaRepository<AutoLongBook, Long>
