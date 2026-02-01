package zygarde.lock.interval

import zygarde.lock.Interval

fun interface IntervalConverter {
  fun toMillis(interval: Interval): Long
}
