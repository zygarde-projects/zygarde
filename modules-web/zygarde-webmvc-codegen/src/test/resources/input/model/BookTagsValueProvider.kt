package codegen.entity

import zygarde.codegen.value.ValueProvider
import zygarde.json.jsonStringToList

class BookTagsValueProvider: ValueProvider<Book, Collection<String>> {
  override fun getValue(v: Book): Collection<String> = v.tags.jsonStringToList()
}
