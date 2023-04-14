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
        MarkCodegenSpec::extraMap1,
        MarkCodegenSpec::extraMap2,
        MarkCodegenSpec::extraMap3,
        MarkCodegenSpec::extraMap4,
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
  var extraMap1: Map<String, Any> = emptyMap()
  var extraMap2: Map<String, Int?> = emptyMap()
  var extraMap3: Map<String, List<String>> = emptyMap()
  var extraMap4: Map<String, List<Int?>> = emptyMap()
}
