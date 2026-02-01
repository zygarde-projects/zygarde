package zygarde.lock

interface Lock {
  fun acquire(keys: List<String>, storeId: String, expiration: Long): String?

  fun release(keys: List<String>, storeId: String, token: String): Boolean

  fun refresh(keys: List<String>, storeId: String, token: String, expiration: Long): Boolean
}
