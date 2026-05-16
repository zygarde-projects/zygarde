package zygarde.codegen.`data`.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import kotlin.Int
import kotlin.String

@Schema
public data class TodoDetailDto(
  @Schema(
    description="",
    requiredMode=Schema.RequiredMode.REQUIRED,
  )
  public var id: Int,
  @Schema(
    description="",
    requiredMode=Schema.RequiredMode.REQUIRED,
  )
  public var description: String,
  @Schema(
    description="",
    requiredMode=Schema.RequiredMode.REQUIRED,
  )
  public var title: String,
  @Schema(
    description="",
    requiredMode=Schema.RequiredMode.REQUIRED,
  )
  public var remark: String,
) : Serializable
