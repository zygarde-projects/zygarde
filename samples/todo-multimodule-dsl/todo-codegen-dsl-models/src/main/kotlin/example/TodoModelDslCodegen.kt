package example

import example.codegen.model.meta.AbstractTodoCodegen
import zygarde.codegen.meta.CodegenDtoSimple

class TodoModelDslCodegen : AbstractTodoCodegen() {

  enum class TodoDtos : CodegenDtoSimple {
    TodoDto,
    CreateTodoReq,
    UpdateTodoReq,
  }

  override fun codegen() {
    mapToDto(TodoDtos.TodoDto) {
      fromAutoIntId(id)
      fromModel(description)
    }

    applyFromDto(TodoDtos.CreateTodoReq) {
      toModel(description)
    }
    applyFromDto(TodoDtos.UpdateTodoReq) {
      toModel(description)
    }
  }
}
