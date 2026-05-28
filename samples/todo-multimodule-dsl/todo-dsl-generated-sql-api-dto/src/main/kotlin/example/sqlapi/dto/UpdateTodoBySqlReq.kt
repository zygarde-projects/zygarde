package example.sqlapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import kotlin.Int
import kotlin.String

@Schema
public data class UpdateTodoBySqlReq(
  @Schema(
    description="Todo description",
    requiredMode=Schema.RequiredMode.REQUIRED,
  )
  public var description: String,
  @Schema(
    description="Todo id",
    requiredMode=Schema.RequiredMode.REQUIRED,
  )
  public var id: Int,
) : Serializable
