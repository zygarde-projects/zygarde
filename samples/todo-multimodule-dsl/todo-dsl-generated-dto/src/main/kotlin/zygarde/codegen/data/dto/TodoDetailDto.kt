package zygarde.codegen.`data`.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import kotlin.Int
import kotlin.String

@Schema
public data class TodoDetailDto(
  @Schema(
    description="",
    required=true
  )
  public var id: Int,
  @Schema(
    description="",
    required=true
  )
  public var description: String,
  @Schema(
    description="",
    required=true
  )
  public var title: String,
  @Schema(
    description="",
    required=true
  )
  public var remark: String
) : Serializable
