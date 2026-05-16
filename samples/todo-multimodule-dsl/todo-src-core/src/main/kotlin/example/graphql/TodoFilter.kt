package example.graphql

data class TodoFilter(
  val idEq: Int? = null,
  val idsIn: Collection<Int>? = null,
  val descriptionContains: String? = null,
)
