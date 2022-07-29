package zygarde.data.search

import io.swagger.v3.oas.annotations.media.Schema

/**
 * @author leo
 */
@Schema
open class SearchNumberRange<T : Number>(
  var from: T? = null,
  var to: T? = null
)

class SearchDoubleRange(from: Double?, to: Double?) : SearchNumberRange<Double>(from, to)
class SearchIntRange(from: Int?, to: Int?) : SearchNumberRange<Int>(from, to)
