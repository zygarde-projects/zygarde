package example.graphql

data class TodoFilter(
  val idEq: Int? = null,
  val descriptionContains: String? = null,
)
