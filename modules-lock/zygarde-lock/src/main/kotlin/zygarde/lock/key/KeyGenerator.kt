package zygarde.lock.key

import java.lang.reflect.Method

interface KeyGenerator {
  fun resolveKeys(lockKeyPrefix: String, expression: String, obj: Any, method: Method, args: Array<Any?>): List<String>
}
