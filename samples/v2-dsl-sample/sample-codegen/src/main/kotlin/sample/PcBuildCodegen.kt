package sample

import sample.PcBuildCodegen.PcBuildSearchReq.SearchPcBuildReq
import zygarde.codegen.dsl.model.internal.DtoFieldValidation
import zygarde.codegen.dsl.model.type.ForceNull
import zygarde.codegen.dsl.model.type.ValueProviderParameterType
import zygarde.codegen.meta.CodegenDtoSimple
import zygarde.codegen.meta.CodegenDtoWithSuperClass
import zygarde.codegen.value.AutoIntIdValueProvider
import zygarde.data.jpa.search.request.PagingAndSortingRequest
import zygarde.generated.model.meta.AbstractPcBuildCodegen
import kotlin.reflect.KClass

class PcBuildCodegen : AbstractPcBuildCodegen() {

  enum class PcBuildDtos : CodegenDtoSimple {
    PcBuildDto,
    PcBuildDetailDto,
  }

  enum class PcBuildSearchReq(override val superClass: KClass<*>) : CodegenDtoWithSuperClass {
    SearchPcBuildReq(PagingAndSortingRequest::class)
  }

  enum class PcBuildCrudReq : CodegenDtoSimple {
    CreatePcBuildReq,
    UpdatePcBuildReq,
  }

  override fun codegen() {
    id {
      comment = "id"
      toDto(*PcBuildDtos.values()) {
        forceNull = ForceNull.NOT_NULL
        valueProvider = AutoIntIdValueProvider::class
        valueProviderParameterType = ValueProviderParameterType.OBJECT
      }
    }

    name {
      comment = "名稱"
      toDto(*PcBuildDtos.values())
      applyFrom(*PcBuildCrudReq.values()) {
        validations.add(DtoFieldValidation.Regex("^[A-Z]{1,10}\$".toRegex(), "name should have length between 1 to 10"))
      }
      fieldFor(SearchPcBuildReq) { forceNull = ForceNull.NOT_NULL }
    }

    description {
      comment = "描述"
      toDto(*PcBuildDtos.values())
      applyFrom(*PcBuildCrudReq.values())
    }

    extraField<Double>("rating", true).toDto(PcBuildDtos.PcBuildDetailDto) {
      comment = "評分"
    }

    extraCollectionField<String>("tags") {
      toDto(PcBuildDtos.PcBuildDetailDto)
      SearchPcBuildReq.fieldFrom(this)
    }

    PcBuildDtos.PcBuildDto.fieldRefToDto(
      fieldName = "mainImage",
      dtoRef = ImageCodegen.ImageModels.ImageDto,
      nullable = true
    ) {
      comment = "Main Image of this build"
    }

    PcBuildDtos.PcBuildDto.fieldRefToDtoCollection(
      fieldName = "otherImages",
      dtoRef = ImageCodegen.ImageModels.ImageDto,
    ) {
      comment = "other images"
    }
  }
}
