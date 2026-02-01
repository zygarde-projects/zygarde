package zygarde.lock.retry

import zygarde.lock.Locked
import org.springframework.retry.support.RetryTemplate

fun interface RetryTemplateConverter {
  fun construct(locked: Locked): RetryTemplate?
}
