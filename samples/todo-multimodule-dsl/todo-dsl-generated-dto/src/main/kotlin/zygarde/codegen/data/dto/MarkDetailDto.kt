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
    required=true
  )
  public var id: Int,
  @Schema(
    description="x",
    required=true
  )
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
  public var comments: Collection<String> = emptyList(),
  @Schema(
    description="extra string",
    required=true
  )
  public var extraStr: String,
  @Schema(
    description="",
    required=true
  )
  public var extraMap1: Map<String, Any>,
  @Schema(
    description="",
    required=true
  )
  public var extraMap2: Map<String, Int?>,
  @Schema(
    description="",
    required=true
  )
  public var extraMap3: Map<String, List<String>>,
  @Schema(
    description="",
    required=true
  )
  public var extraMap4: Map<String, List<Int?>>,
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
