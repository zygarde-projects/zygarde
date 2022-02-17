package zygarde.codegen.model.extensions

import example.Todo
import zygarde.codegen.`data`.dto.CreateTodoReq
import zygarde.codegen.`data`.dto.UpdateTodoReq

public object TodoApplyValueExtensions {
  public fun Todo.applyFrom(req: CreateTodoReq): Todo {
    this.description = req.description
    return this
  }

  public fun Todo.applyFrom(req: UpdateTodoReq): Todo {
    this.description = req.description
    return this
  }
}
