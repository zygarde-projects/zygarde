package zygarde.data.jpa.entity

interface AutoIdGetter<T> {
  val id: T?
}
