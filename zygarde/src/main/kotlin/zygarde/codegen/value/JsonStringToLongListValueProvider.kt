package zygarde.codegen.value

import zygarde.json.jsonStringToList

class JsonStringToLongListValueProvider : ValueProvider<String, List<Long>> {
  override fun getValue(v: String): List<Long> {
    return v.jsonStringToList()
  }
}
