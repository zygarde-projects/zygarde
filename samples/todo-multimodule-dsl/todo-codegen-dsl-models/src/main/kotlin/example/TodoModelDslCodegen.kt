package example

import example.codegen.model.meta.AbstractTodoCodegen
import example.codegen.model.meta.NoteModelFields
import example.codegen.model.meta.TodoModelFields
import zygarde.codegen.meta.CodegenDtoSimple

class TodoModelDslCodegen : AbstractTodoCodegen() {

  enum class TodoDtos : CodegenDtoSimple {
    TodoDto,
    CreateTodoReq,
    UpdateTodoReq,
    TodoDetailDto,
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

    dto(TodoDtos.TodoDetailDto) {
      fromAutoIntId(id)
      from(
        TodoModelFields.description,
        NoteModelFields.title,
      )
      fieldExtra(
        custom<String>("remark")
      )
    }
  }
}
