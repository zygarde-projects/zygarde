package sample

import sample.ImageCodegen.ImageModels.ImageDto
import sample.data.model.EmailValidationInfo
import zygarde.codegen.dsl.model.type.ValueProviderParameterType
import zygarde.codegen.meta.CodegenDtoSimple
import zygarde.codegen.meta.CodegenDtoWithSuperClass
import zygarde.codegen.value.AutoIntIdValueProvider
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
    id {
      toDto(ImageDto) {
        valueProvider = AutoIntIdValueProvider::class
        valueProviderParameterType = ValueProviderParameterType.OBJECT
      }
    }
    url { toDto(ImageDto) }

    extraField<String>("imageBase64").fieldFor(SignUpReqs.UploadImageBeforeSignupReq)
  }
}
