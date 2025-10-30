package zygarde.test.input.comprehensive

import zygarde.codegen.AdditionalDtoProps
import zygarde.codegen.ApiProp
import zygarde.codegen.Dto
import zygarde.codegen.DtoInherit
import zygarde.codegen.DtoInherits
import zygarde.codegen.RequestDto
import zygarde.codegen.SearchType
import zygarde.codegen.ZyModel
import javax.persistence.Transient

@ZyModel
@DtoInherits(
  value = [
    DtoInherit(dto = "UserResponseDto", inherit = zygarde.data.dto.BaseAuditDto::class)
  ]
)
@AdditionalDtoProps(
  props = [
    zygarde.codegen.AdditionalDtoProp(
      field = "_computedValue",
      fieldType = String::class,
      forDto = ["UserResponseDto"],
      comment = "Computed value"
    )
  ]
)
data class User(
  var id: Long,
  
  @ApiProp(
    comment = "User name",
    dto = [Dto(name = "UserResponseDto")],
    requestDto = [RequestDto(name = "CreateUserRequest"), RequestDto(name = "UpdateUserRequest")]
  )
  var name: String,
  
  @ApiProp(
    comment = "User email",
    dto = [Dto(name = "UserResponseDto", fieldName = "emailAddress")],
    requestDto = [
      RequestDto(
        name = "CreateUserRequest", 
        notNullInReq = true,
        applyValueToEntity = true
      ),
      RequestDto(
        name = "UpdateUserRequest",
        applyValueToEntity = true,
        sinceApiVersion = 2
      )
    ]
  )
  var email: String?,
  
  @ApiProp(
    comment = "User age",
    requestDto = [
      RequestDto(
        name = "SearchUserRequest",
        searchType = SearchType.GTE,
        searchForField = "age"
      )
    ]
  )
  var age: Int?,
  
  @ApiProp(
    comment = "User tags",
    dto = [Dto(name = "UserResponseDto", refCollection = true)],
    requestDto = [
      RequestDto(
        name = "SearchUserRequest",
        searchType = SearchType.IN_LIST
      )
    ]
  )
  var tags: Collection<String>,
  
  @Transient
  var _computedValue: String = ""
)
