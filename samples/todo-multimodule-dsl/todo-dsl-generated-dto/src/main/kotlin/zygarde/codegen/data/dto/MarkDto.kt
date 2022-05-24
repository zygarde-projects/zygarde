package zygarde.codegen.`data`.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import kotlin.Int
import kotlin.String
import kotlin.collections.Collection

@Schema
public data class MarkDto(
  @Schema(
    description="",
    required=true
  )
  public val id: Int,
  @Schema(
    description="x",
    required=true
  )
  public val x: Int,
  @Schema(
    description="y",
    required=true
  )
  public val y: Int,
  @Schema(
    description="",
    required=true
  )
  public val comments: Collection<String>,
  @Schema(
    description="extra string",
    required=true
  )
  public val extraStr: String,
  @Schema(
    description="",
    required=true
  )
  public val todo: TodoDto
) : Serializable
