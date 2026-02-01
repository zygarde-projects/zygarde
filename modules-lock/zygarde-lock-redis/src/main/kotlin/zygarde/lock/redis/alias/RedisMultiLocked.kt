package zygarde.lock.redis.alias

import zygarde.lock.Interval
import zygarde.lock.Locked
import zygarde.lock.redis.impl.MultiRedisLock
import java.util.concurrent.TimeUnit
import org.springframework.core.annotation.AliasFor

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Locked(type = MultiRedisLock::class)
annotation class RedisMultiLocked(
  @get:AliasFor(annotation = Locked::class)
  val manuallyReleased: Boolean = false,
  @get:AliasFor(annotation = Locked::class)
  val storeId: String = "distributed_lock",
  @get:AliasFor(annotation = Locked::class)
  val prefix: String = "",
  @get:AliasFor(annotation = Locked::class)
  val expression: String = "#executionPath",
  @get:AliasFor(annotation = Locked::class)
  val expiration: Interval = Interval(value = "10", unit = TimeUnit.SECONDS),
  @get:AliasFor(annotation = Locked::class)
  val timeout: Interval = Interval(value = "1", unit = TimeUnit.SECONDS),
  @get:AliasFor(annotation = Locked::class)
  val retry: Interval = Interval(value = "50"),
  @get:AliasFor(annotation = Locked::class)
  val refresh: Interval = Interval(value = "0"),
  @get:AliasFor(annotation = Locked::class)
  val throwing: Boolean = true
)
