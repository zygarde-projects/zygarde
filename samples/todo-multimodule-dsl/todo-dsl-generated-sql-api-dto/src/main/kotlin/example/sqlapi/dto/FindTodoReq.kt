package example.sqlapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import kotlin.Int

@Schema
public data class FindTodoReq(
  @Schema(
    description="Todo id",
    requiredMode=Schema.RequiredMode.REQUIRED,
  )
  public var id: Int,
) : Serializable
