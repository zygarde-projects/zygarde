package example.sqlapi.dto

import example.FileDto
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import kotlin.Int
import kotlin.String

@Schema
public data class TodoReportDto(
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
    requiredMode=Schema.RequiredMode.NOT_REQUIRED,
  )
  public var `file`: FileDto? = null,
) : Serializable
