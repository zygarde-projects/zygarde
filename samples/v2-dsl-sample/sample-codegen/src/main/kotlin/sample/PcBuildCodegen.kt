package sample

import zygarde.codegen.dsl.model.type.ForceNull
import zygarde.codegen.meta.RegisterDto
import zygarde.codegen.meta.RegisterDtos
import zygarde.codegen.value.AutoIntIdValueProvider
import zygarde.data.jpa.search.request.PagingAndSortingRequest
import zygarde.generated.dto.PcBuildDtos
import zygarde.generated.dto.PcBuildDtos.PcBuildDetailDto
import zygarde.generated.model.meta.AbstractPcBuildCodegen

@RegisterDtos(
  "PcBuild",
  RegisterDto(
    "PcBuildDto",
    "PcBuildDetailDto",
    "CreatePcBuildReq",
    "UpdatePcBuildReq"
  ),
  RegisterDto(
    "SearchPcBuildReq",
    superClass = PagingAndSortingRequest::class
  )
)
class PcBuildCodegen : AbstractPcBuildCodegen() {
  override fun codegen() {
    id.mapToDtos(*PcBuildDtos.values()) {
      forceNull = ForceNull.NOT_NULL
      valueProvider = AutoIntIdValueProvider::class
    }
    name.mapToDtos(*PcBuildDtos.values())
    description.mapToDtos(*PcBuildDtos.values())

    extraField<Double>("rating", true).mapToDtos(PcBuildDetailDto)
    extraCollectionField<String>("tags").mapToDtos(PcBuildDetailDto)
  }
}
