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
  public val id: Int,
  @Schema(
    description="",
    required=true
  )
  public val description: String,
  @Schema(
    description="",
    required=true
  )
  public val title: String,
  @Schema(
    description="",
    required=true
  )
  public val remark: String
) : Serializable
