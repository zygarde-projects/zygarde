package zygarde.data.jpa.search

interface Searchable<F, T> {
  fun fieldName(): String
}
