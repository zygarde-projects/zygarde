package zygarde.lock

import java.util.concurrent.TimeUnit

@Retention(AnnotationRetention.RUNTIME)
annotation class Interval(
  val value: String,
  val unit: TimeUnit = TimeUnit.MILLISECONDS
)
