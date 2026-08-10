package example

import zygarde.codegen.dsl.ModelMappingCodegenSpec
import zygarde.codegen.meta.CodegenDtoSimple
import zygarde.data.api.PagingAndSortingRequest

class TodoModelDslCodegen : ModelMappingCodegenSpec({
  TodoDtos.TodoDto {
    fromAutoIntId(Todo::id)
    from(Todo::description)
    provide<FileDtoProvider, String, FileDto>("file") {
      key(Todo::fileId)
      nullable()
    }
  }

  TodoDtos.CreateTodoReq {
    applyTo(Todo::description)
  }

  TodoDtos.UpdateTodoReq {
    applyTo(Todo::description)
  }

  TodoDtos.SearchTodoReq {
    sortableFields(Todo::id, Todo::description)
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
    SearchTodoReq {
      override fun superClass() = PagingAndSortingRequest::class
    },
    TodoDetailDto,
  }
}
