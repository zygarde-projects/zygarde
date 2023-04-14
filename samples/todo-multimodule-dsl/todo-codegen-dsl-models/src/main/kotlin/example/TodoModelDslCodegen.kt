package example

import zygarde.codegen.dsl.ModelMappingCodegenSpec
import zygarde.codegen.meta.CodegenDtoSimple

class TodoModelDslCodegen : ModelMappingCodegenSpec({
  TodoDtos.TodoDto {
    fromAutoIntId(Todo::id)
    from(Todo::description)
  }

  TodoDtos.CreateTodoReq {
    applyTo(Todo::description)
  }

  TodoDtos.UpdateTodoReq {
    applyTo(Todo::description)
  }

  TodoDtos.TodoDetailDto {
    fromAutoIntId(Todo::id)
    from(
      Todo::description,
      Note::title,
    )
    fromExtra(
      TodoExtraModel::remark
    )
  }
}) {

  class TodoExtraModel {
    var remark: String = ""
  }

  enum class TodoDtos : CodegenDtoSimple {
    TodoDto,
    CreateTodoReq,
    UpdateTodoReq,
    TodoDetailDto,
  }
}
