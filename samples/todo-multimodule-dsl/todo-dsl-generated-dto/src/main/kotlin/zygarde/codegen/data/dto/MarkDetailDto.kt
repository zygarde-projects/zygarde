package zygarde.codegen.`data`.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import kotlin.Any
import kotlin.Int
import kotlin.String
import kotlin.collections.Collection
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.emptyList

@Schema
public data class MarkDetailDto(
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
  public var comments: Collection<String> = emptyList(),
  @Schema(
    description="extra string",
    requiredMode=Schema.RequiredMode.REQUIRED,
  )
  public var extraStr: String,
  @Schema(
    description="",
    requiredMode=Schema.RequiredMode.REQUIRED,
  )
  public var extraMap1: Map<String, Any>,
  @Schema(
    description="",
    requiredMode=Schema.RequiredMode.REQUIRED,
  )
  public var extraMap2: Map<String, Int?>,
  @Schema(
    description="",
    requiredMode=Schema.RequiredMode.REQUIRED,
  )
  public var extraMap3: Map<String, List<String>>,
  @Schema(
    description="",
    requiredMode=Schema.RequiredMode.REQUIRED,
  )
  public var extraMap4: Map<String, List<Int?>>,
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
