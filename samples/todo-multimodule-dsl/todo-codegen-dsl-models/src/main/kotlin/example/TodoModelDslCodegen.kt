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
    dto(TodoDtos.TodoDto) {
      fromAutoIntId(id)
      from(description)
    }

    req(TodoDtos.CreateTodoReq) {
      applyTo(description)
    }

    req(TodoDtos.UpdateTodoReq) {
      applyTo(description)
    }
  }
}
