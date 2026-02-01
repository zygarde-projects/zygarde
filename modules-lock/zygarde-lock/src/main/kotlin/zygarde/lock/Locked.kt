package zygarde.lock

import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Locked(
  val manuallyReleased: Boolean = false,
  val storeId: String = "distributed_lock",
  val prefix: String = "",
  val expression: String = "#executionPath",
  val expiration: Interval = Interval(value = "10", unit = TimeUnit.SECONDS),
  val timeout: Interval = Interval(value = "1", unit = TimeUnit.SECONDS),
  val retry: Interval = Interval(value = "50"),
  val refresh: Interval = Interval(value = "0"),
  val type: KClass<out Lock> = Lock::class,
  val throwing: Boolean = true
)
