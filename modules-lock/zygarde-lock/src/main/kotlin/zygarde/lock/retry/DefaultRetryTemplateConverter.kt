package zygarde.lock.retry

import zygarde.lock.Locked
import zygarde.lock.exception.LockNotAvailableException
import zygarde.lock.interval.IntervalConverter
import org.springframework.retry.RetryPolicy
import org.springframework.retry.backoff.BackOffPolicy
import org.springframework.retry.backoff.FixedBackOffPolicy
import org.springframework.retry.policy.CompositeRetryPolicy
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.policy.TimeoutRetryPolicy
import org.springframework.retry.support.RetryTemplate

class DefaultRetryTemplateConverter(
  private val intervalConverter: IntervalConverter
) : RetryTemplateConverter {
  override fun construct(locked: Locked): RetryTemplate? {
    val retryTemplate = RetryTemplate()
    val retryPolicy = resolveLockRetryPolicy(locked) ?: return null
    val backOffPolicy = resolveBackOffPolicy(locked) ?: return null
    retryTemplate.setRetryPolicy(retryPolicy)
    retryTemplate.setBackOffPolicy(backOffPolicy)
    return retryTemplate
  }

  private fun resolveLockRetryPolicy(locked: Locked): RetryPolicy? {
    val timeoutRetryPolicy = resolveTimeoutRetryPolicy(locked) ?: return null
    val exceptionTypeRetryPolicy = resolveExceptionTypeRetryPolicy()
    return CompositeRetryPolicy().apply {
      setPolicies(arrayOf(timeoutRetryPolicy, exceptionTypeRetryPolicy))
    }
  }

  private fun resolveTimeoutRetryPolicy(locked: Locked): RetryPolicy? {
    val timeout = intervalConverter.toMillis(locked.timeout)
    if (timeout <= 0) return null
    return TimeoutRetryPolicy().apply { setTimeout(timeout) }
  }

  private fun resolveExceptionTypeRetryPolicy(): RetryPolicy {
    return SimpleRetryPolicy(Int.MAX_VALUE, mapOf(LockNotAvailableException::class.java to true))
  }

  private fun resolveBackOffPolicy(locked: Locked): BackOffPolicy? {
    val retry = intervalConverter.toMillis(locked.retry)
    if (retry <= 0) return null
    return FixedBackOffPolicy().apply { backOffPeriod = retry }
  }
}
