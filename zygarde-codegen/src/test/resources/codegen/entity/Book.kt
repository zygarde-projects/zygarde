package codegen.entity

import codegen.spec.AuthorApiSpec
import codegen.spec.BookApiSpec.Companion.DTO_BOOK
import codegen.spec.BookApiSpec.Companion.DTO_BOOK_DETAIL
import codegen.spec.BookApiSpec.Companion.REQ_BOOK_CREATE
import codegen.spec.BookApiSpec.Companion.REQ_BOOK_SEARCH
import zygarde.codegen.*
import zygarde.codegen.value.AutoIntIdValueProvider
import zygarde.codegen.value.JsonStringToLongListValueProvider
import zygarde.codegen.value.ToJsonStringValueProvider
import zygarde.data.api.ApiSearchKeyword
import zygarde.data.jpa.entity.AutoIntIdEntity
import zygarde.data.jpa.search.request.PagingAndSortingRequest
import zygarde.data.jpa.search.request.SearchDateTimeRange
import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.Lob
import javax.persistence.ManyToOne
import javax.persistence.Transient

@Entity
@AdditionalDtoProps(
  [
    AdditionalDtoProp(
      forDto = [DTO_BOOK, DTO_BOOK_DETAIL],
      field = "id",
      fieldType = Int::class,
      comment = "id of Book",
      entityValueProvider = AutoIntIdValueProvider::class
    )
  ]
)
@DtoInherits(
  [
    DtoInherit(dto = REQ_BOOK_SEARCH, inherit = PagingAndSortingRequest::class)
  ]
)
@ZyModel
class Book(
  @ApiProp(
    dto = [Dto(DTO_BOOK), Dto(DTO_BOOK_DETAIL)],
    requestDto = [
      RequestDto(REQ_BOOK_CREATE),
      RequestDto(REQ_BOOK_SEARCH, searchType = SearchType.EQ),
      RequestDto(REQ_BOOK_SEARCH, fieldName = "nameList", refClass = String::class, refCollection = true, searchType = SearchType.LIST_CONTAINS_ANY)
    ],
    comment = "name of book"
  )
  var name: String = "",

  @ApiProp(
    dto = [Dto(DTO_BOOK), Dto(DTO_BOOK_DETAIL)],
    requestDto = [RequestDto(REQ_BOOK_CREATE), RequestDto(
      REQ_BOOK_SEARCH,
      searchType = SearchType.GT
    )],
    comment = "price of book"
  )
  var price: Int = 0,

  @ApiProp(dto = [Dto(DTO_BOOK_DETAIL)], requestDto = [RequestDto(REQ_BOOK_SEARCH, searchType = SearchType.LT)])
  var priceD: Double? = null,

  @ApiProp(dto = [Dto(DTO_BOOK_DETAIL)], requestDto = [RequestDto(REQ_BOOK_SEARCH, searchType = SearchType.GTE)])
  var priceF: Float? = null,

  @ApiProp(dto = [Dto(DTO_BOOK_DETAIL)], requestDto = [RequestDto(REQ_BOOK_SEARCH, searchType = SearchType.LTE)])
  var priceS: Short? = null,

  @ApiProp(
    dto = [
      Dto(DTO_BOOK, ref = AuthorApiSpec.DTO_AUTHOR),
      Dto(DTO_BOOK_DETAIL, ref = AuthorApiSpec.DTO_AUTHOR)
    ],
    comment = "author of book"
  )
  @ManyToOne(targetEntity = Author::class)
  var author: Author? = null,

  @ApiProp(
    dto = [
      Dto(DTO_BOOK, ref = AuthorApiSpec.DTO_AUTHOR),
      Dto(DTO_BOOK_DETAIL, ref = AuthorApiSpec.DTO_AUTHOR)
    ],
    comment = "recommend author of book"
  )
  @ManyToOne(targetEntity = Author::class)
  var recommendAuthor: Author? = null,

  @ApiProp(
    dto = [
      Dto(DTO_BOOK),
      Dto(name = DTO_BOOK_DETAIL, fieldName = "releaseDateTime")
    ],
    requestDto = [
      RequestDto(REQ_BOOK_SEARCH, refClass = SearchDateTimeRange::class, searchType = SearchType.DATE_TIME_RANGE)
    ]
  )
  var releaseAt: LocalDateTime = LocalDateTime.now(),

  @ApiProp(
    dto = [
      Dto(
        name = DTO_BOOK_DETAIL,
        refClass = String::class,
        refCollection = true,
        entityValueProvider = BookTagsValueProvider::class
      )
    ],
    requestDto = [
      RequestDto(
        REQ_BOOK_CREATE,
        refClass = String::class,
        refCollection = true,
        valueProvider = ToJsonStringValueProvider::class
      )
    ]
  )
  @Lob
  var tags: String,
  @ApiProp(
    dto = [
      Dto(
        name = DTO_BOOK_DETAIL,
        refClass = Long::class,
        refCollection = true,
        valueProvider = JsonStringToLongListValueProvider::class
      )
    ]
  )
  @Lob
  var categoryIds: String,

  var user: User? = null
) : AutoIntIdEntity() {

  @ApiProp(
    requestDto = [
      RequestDto(
        REQ_BOOK_SEARCH,
        refClass = ApiSearchKeyword::class,
        searchType = SearchType.KEYWORD,
        searchForField = "name"
      )
    ]
  )
  @Transient
  var _nameKeyWord: ApiSearchKeyword? = null

  @ApiProp(
    requestDto = [
      RequestDto(
        REQ_BOOK_SEARCH,
        refClass = String::class,
        searchType = SearchType.STARTS_WITH,
        searchForField = "name"
      )
    ]
  )
  @Transient
  var _nameStartsWith: String? = null

  @ApiProp(
    requestDto = [
      RequestDto(
        REQ_BOOK_SEARCH,
        refClass = String::class,
        searchType = SearchType.ENDS_WITH,
        searchForField = "name"
      )
    ]
  )
  @Transient
  var _nameEndsWith: String? = null

  @ApiProp(
    requestDto = [
      RequestDto(
        REQ_BOOK_SEARCH,
        refClass = String::class,
        searchType = SearchType.CONTAINS,
        searchForField = "name"
      )
    ]
  )
  @Transient
  var _nameContains: String? = null

  @ApiProp(
    requestDto = [
      RequestDto(
        REQ_BOOK_SEARCH,
        refClass = String::class,
        refCollection = true,
        searchType = SearchType.IN_LIST,
        searchForField = "name"
      )
    ]
  )
  @Transient
  var _names: Set<String>? = null

  @ApiProp(
    requestDto = [
      RequestDto(REQ_BOOK_SEARCH, applyValueToEntity = false)
    ]
  )
  @Transient
  var _released: Boolean? = null
}
