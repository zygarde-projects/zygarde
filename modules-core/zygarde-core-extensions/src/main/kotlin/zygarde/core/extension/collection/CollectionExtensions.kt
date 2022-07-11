package zygarde.core.extension.collection

object CollectionExtensions {
  fun <T> Collection<T>.takeUntilFirstOccur(includingOccur: Boolean = true, predicate: (T) -> Boolean): Collection<T> {
    val idx = this.indexOfFirst(predicate)
    return if (idx == -1) {
      this
    } else {
      this.take(
        if (includingOccur) {
          idx + 1
        } else {
          idx
        }
      )
    }
  }
}
