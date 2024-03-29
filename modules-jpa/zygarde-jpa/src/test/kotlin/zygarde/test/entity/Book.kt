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
  var createdAt: LocalDateTime = LocalDateTime.now(),
  var aId: Int? = null,
  var minPrice: Int = (price * 0.8).toInt(),
  var maxPrice: Int = (price * 1.2).toInt(),
  var status: BookStatus = BookStatus.ON_SALE
) : AutoLongIdEntity()

enum class BookStatus {
  ON_SALE, SOLD_OUT
}
