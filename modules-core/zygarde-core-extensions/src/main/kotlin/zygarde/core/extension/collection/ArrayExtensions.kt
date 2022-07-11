package zygarde.core.extension.collection

object ArrayExtensions {
  fun <T> Array<T>.takeUntilFirstOccur(includingOccur: Boolean = true, predicate: (T) -> Boolean): List<T> {
    val idx = this.indexOfFirst(predicate)
    return if (idx == -1) {
      this.toList()
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
