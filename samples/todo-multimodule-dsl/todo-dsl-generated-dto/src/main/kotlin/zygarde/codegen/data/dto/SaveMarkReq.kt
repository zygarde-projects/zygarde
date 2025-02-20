package zygarde.codegen.`data`.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import javax.validation.constraints.DecimalMax
import javax.validation.constraints.NotEmpty
import kotlin.Int
import kotlin.String

@Schema
public data class SaveMarkReq(
  @Schema(
    description="x",
    required=true
  )
  @field:NotEmpty
  @field:DecimalMax(value = "100")
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
  public var todo: TodoDto,
  @Schema(
    description="",
    required=true
  )
  public var longRemark: String
) : Serializable
