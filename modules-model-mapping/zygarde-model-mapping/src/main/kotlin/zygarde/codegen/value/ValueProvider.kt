package zygarde.codegen.value

interface ValueProvider<E, T> {
  fun getValue(v: E): T
}
