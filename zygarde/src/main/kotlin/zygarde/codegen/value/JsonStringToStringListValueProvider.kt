package zygarde.codegen.value

import zygarde.json.jsonStringToList

class JsonStringToStringListValueProvider : ValueProvider<String, List<String>> {
  override fun getValue(v: String): List<String> {
    return v.jsonStringToList()
  }
}
