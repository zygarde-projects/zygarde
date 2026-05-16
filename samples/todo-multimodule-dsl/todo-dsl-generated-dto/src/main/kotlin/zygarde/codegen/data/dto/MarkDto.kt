package zygarde.codegen.`data`.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import kotlin.Int
import kotlin.String

@Schema
public data class MarkDto(
  @Schema(
    description="",
    requiredMode=Schema.RequiredMode.REQUIRED,
  )
  public var id: Int,
  @Schema(
    description="x",
    requiredMode=Schema.RequiredMode.REQUIRED,
  )
  public var x: Int,
  @Schema(
    description="y",
    requiredMode=Schema.RequiredMode.REQUIRED,
  )
  public var y: Int,
  @Schema(
    description="",
    requiredMode=Schema.RequiredMode.REQUIRED,
  )
  public var longRemark: String,
) : Serializable
