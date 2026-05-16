package zygarde.codegen.`data`.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import kotlin.String

@Schema
public data class CreateTodoReq(
  @Schema(
    description="",
    requiredMode=Schema.RequiredMode.REQUIRED,
  )
  public var description: String,
) : Serializable
