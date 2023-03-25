package zygarde.codegen.`data`.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import kotlin.Int

@Schema
public data class MarkDto(
  @Schema(
    description="",
    required=true
  )
  public var id: Int,
  @Schema(
    description="x",
    required=true
  )
  public var x: Int,
  @Schema(
    description="y",
    required=true
  )
  public var y: Int
) : Serializable
