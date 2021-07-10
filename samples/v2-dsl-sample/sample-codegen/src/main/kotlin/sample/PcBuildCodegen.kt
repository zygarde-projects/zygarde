package sample

import zygarde.codegen.dsl.DslModelMappingCodegen
import zygarde.codegen.dsl.model.type.ForceNull
import zygarde.codegen.meta.RegisterDto
import zygarde.codegen.meta.RegisterDtos
import zygarde.data.jpa.search.request.PagingAndSortingRequest
import zygarde.generated.dto.PcBuildDtos
import zygarde.generated.model.meta.PcBuildMeta

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
class PcBuildCodegen : DslModelMappingCodegen() {
  override fun codegen() {
    PcBuildMeta.id.mapToDtos(
      forceNull = ForceNull.NOT_NULL,
      *PcBuildDtos.values()
    )
    PcBuildMeta.name.mapToDtos(
      *PcBuildDtos.values()
    )
    PcBuildMeta.description.mapToDtos(
      *PcBuildDtos.values()
    )
  }
}
