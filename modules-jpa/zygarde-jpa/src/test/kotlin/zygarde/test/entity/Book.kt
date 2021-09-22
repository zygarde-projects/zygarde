package zygarde.test.entity

import zygarde.data.jpa.entity.AutoLongIdEntity
import java.time.LocalDate
import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.ManyToOne

@Entity
open class Book(
  var name: String = "",
  var price: Int = 100,
  @ManyToOne(targetEntity = Author::class, fetch = FetchType.LAZY)
  open var author: Author? = null,
  var releaseDate: LocalDate = LocalDate.now(),
  var createdAt: LocalDateTime = LocalDateTime.now()
) : AutoLongIdEntity()
