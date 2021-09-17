package zygarde.codegen.value

import zygarde.json.jsonStringToList

class JsonStringToIntListValueProvider : ValueProvider<String, List<Int>> {
  override fun getValue(v: String): List<Int> {
    return v.jsonStringToList()
  }
}
