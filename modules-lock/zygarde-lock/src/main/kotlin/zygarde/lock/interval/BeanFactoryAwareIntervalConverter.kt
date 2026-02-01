package zygarde.lock.interval

import zygarde.lock.Interval
import org.springframework.beans.factory.config.ConfigurableBeanFactory

class BeanFactoryAwareIntervalConverter(
  private val beanFactory: ConfigurableBeanFactory
) : IntervalConverter {
  override fun toMillis(interval: Interval): Long {
    val resolved = resolveValue(interval)
    return convertToMilliseconds(interval, resolved)
  }

  private fun resolveValue(interval: Interval): String {
    val value = beanFactory.resolveEmbeddedValue(interval.value)
    require(!value.isNullOrBlank()) { "Cannot convert interval $interval to milliseconds" }
    return value
  }

  private fun convertToMilliseconds(interval: Interval, value: String): Long {
    try {
      return interval.unit.toMillis(value.toLong())
    } catch (e: NumberFormatException) {
      throw IllegalArgumentException("Cannot convert interval $interval to milliseconds", e)
    }
  }
}
