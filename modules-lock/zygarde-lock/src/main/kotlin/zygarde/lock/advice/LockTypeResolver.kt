package zygarde.lock.advice

import zygarde.lock.Lock

fun interface LockTypeResolver {
  fun get(type: Class<out Lock>): Lock
}
