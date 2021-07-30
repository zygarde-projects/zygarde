package sample

import sample.ImageCodegen.ImageModels.ImageDto
import sample.data.model.EmailValidationInfo
import zygarde.codegen.meta.CodegenDtoSimple
import zygarde.codegen.meta.CodegenDtoWithSuperClass
import zygarde.generated.model.meta.AbstractImageCodegen
import kotlin.reflect.KClass

class ImageCodegen : AbstractImageCodegen() {

  enum class ImageModels : CodegenDtoSimple {
    ImageDto,
  }

  enum class SignUpReqs(override val superClass: KClass<*>) : CodegenDtoWithSuperClass {
    UploadImageBeforeSignupReq(EmailValidationInfo::class)
  }

  override fun codegen() {
    id { toDto(ImageDto) }
    url { toDto(ImageDto) }

    extraField<String>("imageBase64").fieldFor(SignUpReqs.UploadImageBeforeSignupReq)
  }
}
