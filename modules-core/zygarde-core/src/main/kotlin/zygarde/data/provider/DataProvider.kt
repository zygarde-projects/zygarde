package zygarde.data.provider

interface DataProvider<K : Any, V> {
  fun load(keys: Collection<K>, context: DataProviderContext = DataProviderContext.EMPTY): Map<K, V>
}
