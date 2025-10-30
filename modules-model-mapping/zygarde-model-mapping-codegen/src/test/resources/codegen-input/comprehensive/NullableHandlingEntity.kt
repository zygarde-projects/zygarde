package test.entity

import zygarde.codegen.ApiProp
import zygarde.codegen.Dto
import zygarde.codegen.RequestDto
import zygarde.codegen.ZyModel

@ZyModel
data class NullableHandlingEntity(
  @ApiProp(
    comment = "Force not null",
    requestDto = [RequestDto(name = "NullableReq", notNullInReq = true, applyValueToEntity = true)]
  )
  var forceNotNull: String?,

  @ApiProp(
    comment = "Force nullable",
    requestDto = [RequestDto(name = "ForceNullableReq", forceNullableInReq = true, applyValueToEntity = true)]
  )
  var forceNullable: String,

  @ApiProp(
    comment = "Nullable DTO field",
    dto = [Dto(name = "NullableDto", applyValueFromEntity = true)]
  )
  val nullableField: String?,

  @ApiProp(
    comment = "Required DTO field",
    dto = [Dto(name = "NullableDto", applyValueFromEntity = true)]
  )
  val requiredField: String
)
