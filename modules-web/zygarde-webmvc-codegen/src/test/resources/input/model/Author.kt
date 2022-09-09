package codegen.entity

import codegen.spec.AuthorApiSpec.Companion.DTO_AUTHOR
import codegen.spec.AuthorApiSpec.Companion.DTO_AUTHOR_DETAIL
import codegen.spec.BookApiSpec.Companion.DTO_BOOK
import zygarde.codegen.AdditionalDtoProp
import zygarde.codegen.AdditionalDtoProps
import zygarde.codegen.ApiProp
import zygarde.codegen.Dto
import zygarde.codegen.ZyModel
import zygarde.codegen.value.AutoLongIdValueProvider
import zygarde.data.jpa.entity.AutoLongIdEntity
import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.MappedSuperclass
import javax.persistence.OneToMany
import javax.persistence.Transient

@MappedSuperclass
abstract class FooEntity : AutoLongIdEntity() {
  var createdAt: LocalDateTime? = null
}

enum class BookType {
  NORMAL,
  MAGZINE
}

@Entity
@AdditionalDtoProps(
  [
    AdditionalDtoProp(
      forDto = [DTO_AUTHOR, DTO_AUTHOR_DETAIL],
      field = "id",
      fieldType = Long::class,
      comment = "id of Author",
      entityValueProvider = AutoLongIdValueProvider::class
    )
  ]
)
@ZyModel
class Author(
  @ApiProp(
    dto = [Dto(names = [DTO_AUTHOR, DTO_AUTHOR_DETAIL])]
  )
  var name: String = "",
  @ApiProp(
    dto = [Dto(DTO_AUTHOR), Dto(DTO_AUTHOR_DETAIL)]
  )
  var country: String = "",
  @ApiProp(
    dto = [Dto(DTO_AUTHOR_DETAIL, ref = DTO_BOOK, refCollection = true)]
  )
  @OneToMany(targetEntity = Book::class, mappedBy = "author")
  val books: MutableSet<Book> = mutableSetOf()
) : FooEntity() {
  @Transient
  @ApiProp(
    dto = [Dto(DTO_AUTHOR_DETAIL)]
  )
  var _map: Map<BookType, Int> = emptyMap()

  @Transient
  @ApiProp(
    dto = [Dto(DTO_AUTHOR_DETAIL)]
  )
  var _bookTypes: List<BookType> = emptyList()

  @Transient
  @ApiProp(
    dto = [Dto(DTO_AUTHOR_DETAIL)]
  )
  var _vacationMonths: Collection<Int> = emptyList()
}
