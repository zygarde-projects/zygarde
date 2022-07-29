package zygarde.codegen.`data`.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import kotlin.Int

@Schema
public data class SaveMarkReq(
  @Schema(
    description = "x",
    required = true
  )
  public val x: Int,
  @Schema(
    description = "y",
    required = true
  )
  public val y: Int,
  @Schema(
    description = "",
    required = true
  )
  public val todo: TodoDto
) : Serializable
