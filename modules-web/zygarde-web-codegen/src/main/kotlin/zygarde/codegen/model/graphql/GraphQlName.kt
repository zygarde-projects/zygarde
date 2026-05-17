package zygarde.codegen.model.graphql

private val graphQlNameRegex = Regex("[_A-Za-z][_0-9A-Za-z]*")

fun requireGraphQlName(name: String, label: String) {
  require(graphQlNameRegex.matches(name)) {
    "$label must be a valid GraphQL name"
  }
  require(!name.startsWith("__")) {
    "$label must not start with '__' because GraphQL reserves introspection names"
  }
}

fun requireUniqueGraphQlName(name: String, existingNames: Iterable<String>, label: String) {
  require(name !in existingNames) {
    "$label '$name' is already declared"
  }
}

fun requireGraphQlDescription(description: String?, label: String) {
  if (description != null) {
    require(description.isNotBlank()) {
      "$label description must not be blank"
    }
  }
}
