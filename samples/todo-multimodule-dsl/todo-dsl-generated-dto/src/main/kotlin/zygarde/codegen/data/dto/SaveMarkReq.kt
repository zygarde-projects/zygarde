package zygarde.codegen.`data`.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import kotlin.Int

@Schema
public data class SaveMarkReq(
  @Schema(
    description="x",
    required=true
  )
  public var x: Int,
  @Schema(
    description="y",
    required=true
  )
  public var y: Int,
  @Schema(
    description="",
    required=true
  )
  public var todo: TodoDto
) : Serializable
