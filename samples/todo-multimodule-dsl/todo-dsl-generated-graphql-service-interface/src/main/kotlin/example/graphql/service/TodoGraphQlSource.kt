package example.graphql.service

import kotlin.Int
import kotlin.String

public data class TodoGraphQlSource(
  public val id: Int,
  public val description: String,
  public val fileId: String?,
)
