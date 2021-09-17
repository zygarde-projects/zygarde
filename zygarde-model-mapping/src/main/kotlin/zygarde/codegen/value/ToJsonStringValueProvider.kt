package zygarde.codegen.value

import zygarde.json.toJsonString

class ToJsonStringValueProvider : ValueProvider<Any, String> {
  override fun getValue(v: Any): String = v.toJsonString()
}
