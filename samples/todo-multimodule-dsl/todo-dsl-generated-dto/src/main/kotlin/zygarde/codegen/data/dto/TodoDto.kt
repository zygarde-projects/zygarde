package zygarde.codegen.`data`.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import kotlin.Int
import kotlin.String

@Schema
public data class TodoDto(
  @Schema(
    description="",
    required=true
  )
  public var id: Int,
  @Schema(
    description="",
    required=true
  )
  public var description: String
) : Serializable
