package zygarde.codegen.`data`.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.NotEmpty
import java.io.Serializable
import kotlin.Int
import kotlin.String

@Schema
public data class SaveMarkReq(
  @Schema(
    description="x",
    requiredMode=Schema.RequiredMode.REQUIRED,
  )
  @field:NotEmpty
  @field:DecimalMax(value = "100")
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
  public var todo: TodoDto,
  @Schema(
    description="",
    requiredMode=Schema.RequiredMode.REQUIRED,
  )
  public var longRemark: String,
) : Serializable
