package zygarde.codegen.`data`.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import kotlin.Int
import kotlin.String
import kotlin.collections.Collection
import kotlin.collections.emptyList

@Schema
public data class MarkDetailDto(
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
  public var y: Int,
  @Schema(
    description="",
    required=true
  )
  public var comments: Collection<String> = emptyList(),
  @Schema(
    description="extra string",
    required=true
  )
  public var extraStr: String,
  @Schema(
    description="",
    required=true
  )
  public var todo: TodoDto
) : Serializable
