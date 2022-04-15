package zygarde.codegen.`data`.dto

import example.Note
import example.Todo
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import kotlin.Int
import kotlin.String
import zygarde.codegen.`value`.AutoIntIdValueProvider

@Schema
public data class TodoDetailDto(
  @Schema(
    description="",
    required=true
  )
  public val id: Int,
  @Schema(
    description="",
    required=true
  )
  public val description: String,
  @Schema(
    description="",
    required=true
  )
  public val title: String,
  @Schema(
    description="",
    required=true
  )
  public val remark: String
) : Serializable {
  public constructor(
    todo: Todo,
    note: Note,
    extraValues: TodoDetailDtoCompoundExtraValues
  ) : this(id = AutoIntIdValueProvider().getValue(todo)
  , description = todo.description
  , title = note.title
  , remark = extraValues.remark
  )
}
