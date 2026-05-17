package zygarde.codegen.model.graphql

private val graphQlNameRegex = Regex("[_A-Za-z][_0-9A-Za-z]*")

fun requireGraphQlName(name: String, label: String) {
  require(graphQlNameRegex.matches(name)) {
    "$label must be a valid GraphQL name"
  }
}
