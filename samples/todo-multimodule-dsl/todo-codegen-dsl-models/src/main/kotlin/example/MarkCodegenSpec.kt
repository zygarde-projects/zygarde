package example

import example.MarkModels.MarkDto
import example.MarkModels.SaveMarkReq
import zygarde.codegen.dsl.ModelMappingCodegenSpec
import zygarde.codegen.meta.CodegenDtoSimple
import zygarde.codegen.meta.Comment

enum class MarkModels : CodegenDtoSimple {
  MarkDto,
  SaveMarkReq,
}

class MarkCodegenSpec : ModelMappingCodegenSpec({
  MarkDto {
    fromAutoIntId(Mark::id)
    from(
      Mark::x,
      Mark::y,
      Mark::comments,
    )
    fromExtra(
      MarkCodegenSpec::extraStr,
    )
    fromRef("todo", TodoModelDslCodegen.TodoDtos.TodoDto)
  }
  SaveMarkReq {
    applyTo(
      Mark::x,
      Mark::y,
    )
    fieldRef("todo", TodoModelDslCodegen.TodoDtos.TodoDto)
  }
}) {
  @Comment("extra string")
  var extraStr: String = ""
}
