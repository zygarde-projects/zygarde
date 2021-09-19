package codegen.entity

import org.springframework.web.bind.annotation.RequestMethod
import zygarde.codegen.*
import zygarde.data.jpa.entity.AutoLongIdEntity
import javax.persistence.Entity
import javax.persistence.Transient

private const val UserApi = "UserApi"
private const val PhoneService = "PhoneService"
private const val REQ_SEND_VALIDATE_PHONE = "SendPhoneValidationRequest"
private const val RES_SEND_VALIDATE_PHONE = "PhoneValidation"
private const val REQ_DO_VALIDATE_PHONE = "ValidatePhoneRequest"

@ZyApi(
  api = [
    GenApi(
      method = RequestMethod.POST,
      path = "/p/api/phoneValidation",
      api = "$UserApi.sendPhoneValidation",
      apiDescription = "發送簡訊驗證碼",
      service = "$PhoneService.sendPhoneValidation",
      reqRef = REQ_SEND_VALIDATE_PHONE,
      resRef = RES_SEND_VALIDATE_PHONE
    ),
    GenApi(
      method = RequestMethod.PUT,
      path = "/p/api/phoneValidation",
      api = "$UserApi.doValidatePhone",
      apiDescription = "驗證手機驗證碼",
      service = "$PhoneService.doValidatePhone",
      reqRef = REQ_DO_VALIDATE_PHONE,
      resRef = ""
    )
  ]
)
@Entity
@ZyModel
class User(
  @ApiProp(
    comment = "手機號碼",
    requestDto = [
      RequestDto(REQ_SEND_VALIDATE_PHONE, applyValueToEntity = false)
    ]
  )
  val phone: String = "",
  val pwd: String = ""
) : AutoLongIdEntity() {

  @Transient
  @ApiProp(
    comment = "驗證Token",
    dto = [
      Dto(RES_SEND_VALIDATE_PHONE, applyValueFromEntity = false)
    ],
    requestDto = [
      RequestDto(REQ_DO_VALIDATE_PHONE, applyValueToEntity = false)
    ]
  )
  val _phoneValidationToken: String = ""

  @Transient
  @ApiProp(
    comment = "驗證碼",
    requestDto = [
      RequestDto(REQ_DO_VALIDATE_PHONE, applyValueToEntity = false)
    ]
  )
  val _phoneValidationCode: String = ""
}
