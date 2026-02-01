package zygarde.lock.retry

import zygarde.lock.Lock
import zygarde.lock.Locked

interface RetriableLockFactory {
  fun generate(lock: Lock, locked: Locked): Lock
}
