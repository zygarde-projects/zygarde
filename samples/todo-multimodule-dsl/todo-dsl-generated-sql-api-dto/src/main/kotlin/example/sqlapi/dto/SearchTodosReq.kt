package example.sqlapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import kotlin.String

@Schema
public data class SearchTodosReq(
  @Schema(
    description="Search keyword",
    requiredMode=Schema.RequiredMode.NOT_REQUIRED,
  )
  public var keyword: String? = null,
) : Serializable
