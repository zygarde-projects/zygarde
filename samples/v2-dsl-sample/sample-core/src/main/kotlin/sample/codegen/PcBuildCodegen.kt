package sample.codegen

import zygarde.codegen.meta.RegisterDto
import zygarde.codegen.meta.RegisterDtos

@RegisterDtos(
  "PcBuild",
  RegisterDto("CreatePcBuildReq", "UpdatePcBuildReq")
)
object PcBuildCodegen
