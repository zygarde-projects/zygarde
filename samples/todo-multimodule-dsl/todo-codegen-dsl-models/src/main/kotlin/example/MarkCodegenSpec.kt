package example

import example.MarkModels.MarkDetailDto
import example.MarkModels.MarkDto
import example.MarkModels.SaveMarkReq
import zygarde.codegen.dsl.ModelMappingCodegenSpec
import zygarde.codegen.meta.CodegenDtoSimple
import zygarde.core.annotation.Comment

enum class MarkModels : CodegenDtoSimple {
  MarkDto,
  MarkDetailDto,
  SaveMarkReq,
}

class MarkCodegenSpec : ModelMappingCodegenSpec({
  group(MarkDto, MarkDetailDto) {
    fromAutoIntId(Mark::id)
    from(
      Mark::x,
      Mark::y,
    )
    MarkDetailDto {
      from(
        Mark::comments,
      )
      fromExtra(
        MarkCodegenSpec::extraStr,
        MarkCodegenSpec::extraMap,
      )
      fromRef("todo", TodoModelDslCodegen.TodoDtos.TodoDto)
    }
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
  var extraMap: Map<String, List<String>> = emptyMap()
}
