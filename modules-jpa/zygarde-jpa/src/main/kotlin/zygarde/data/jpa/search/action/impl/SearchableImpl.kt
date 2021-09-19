package zygarde.data.jpa.search.action.impl

import zygarde.data.jpa.search.Searchable

class SearchableImpl<F, T>(
  private val fieldName: String
) : Searchable<F, T> {

  override fun fieldName(): String = fieldName
}
