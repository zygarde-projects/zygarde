package example

import example.codegen.model.meta.AbstractTodoCodegen
import zygarde.codegen.dsl.model.type.ValueProviderParameterType
import zygarde.codegen.meta.CodegenDtoSimple
import zygarde.codegen.value.AutoIntIdValueProvider

class TodoModelDslCodegen : AbstractTodoCodegen() {

  enum class TodoDtos : CodegenDtoSimple {
    TodoDto,
    CreateTodoReq,
    UpdateTodoReq,
  }

  override fun codegen() {
    id {
      toDto(TodoDtos.TodoDto) {
        valueProvider = AutoIntIdValueProvider::class
        valueProviderParameterType = ValueProviderParameterType.OBJECT
      }
    }

    description {
      toDto(TodoDtos.TodoDto)
      applyFrom(TodoDtos.CreateTodoReq, TodoDtos.UpdateTodoReq)
    }
  }
}
