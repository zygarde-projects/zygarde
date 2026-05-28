package example.sqlapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import kotlin.Int
import kotlin.String

@Schema
public data class PageTodosReq(
  @Schema(
    description="Search keyword",
    requiredMode=Schema.RequiredMode.NOT_REQUIRED,
  )
  public var keyword: String? = null,
  @Schema(
    description="Page size",
    requiredMode=Schema.RequiredMode.REQUIRED,
  )
  public var pageSize: Int,
  @Schema(
    description="Page index",
    requiredMode=Schema.RequiredMode.REQUIRED,
  )
  public var atPage: Int,
) : Serializable
