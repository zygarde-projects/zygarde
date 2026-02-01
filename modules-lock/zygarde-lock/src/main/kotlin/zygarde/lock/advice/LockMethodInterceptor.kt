package zygarde.lock.advice

import zygarde.lock.Lock
import zygarde.lock.Locked
import zygarde.lock.exception.DistributedLockException
import zygarde.lock.interval.IntervalConverter
import zygarde.lock.key.KeyGenerator
import zygarde.lock.retry.RetriableLockFactory
import java.time.Instant
import java.util.Date
import java.util.concurrent.ScheduledFuture
import org.aopalliance.intercept.MethodInterceptor
import org.aopalliance.intercept.MethodInvocation
import org.slf4j.LoggerFactory
import org.springframework.aop.support.AopUtils
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.scheduling.TaskScheduler

class LockMethodInterceptor(
  private val keyGenerator: KeyGenerator,
  private val lockTypeResolver: LockTypeResolver,
  private val intervalConverter: IntervalConverter,
  private val retriableLockFactory: RetriableLockFactory,
  private val taskScheduler: TaskScheduler?
) : MethodInterceptor {
  private val log = LoggerFactory.getLogger(LockMethodInterceptor::class.java)

  override fun invoke(invocation: MethodInvocation): Any? {
    val context = LockContext(invocation)
    try {
      return executeLockedMethod(invocation, context)
    } catch (e: DistributedLockException) {
      if (!context.locked.throwing) {
        if (context.method.returnType.isPrimitive) {
          throw DistributedLockException(
            "Cannot return null for primitive return type ${context.method.returnType.name} on method ${context.method}. " +
              "Use a nullable return type or set throwing=true.",
            e
          )
        }
        log.warn("Cannot obtain lock for keys {} in store {}", context.keys, context.locked.storeId, e)
        return null
      }
      throw e
    } finally {
      cleanAfterExecution(context)
    }
  }

  private fun executeLockedMethod(invocation: MethodInvocation, context: LockContext): Any? {
    val expiration = intervalConverter.toMillis(context.locked.expiration)
    try {
      val lock = retriableLockFactory.generate(context.lock, context.locked)
      val token = lock.acquire(context.keys, context.locked.storeId, expiration)
      if (token.isNullOrEmpty()) {
        throw IllegalStateException("No token acquired")
      }
      context.token = token
    } catch (e: Exception) {
      throw DistributedLockException("Unable to acquire lock with expression: ${context.locked.expression}", e)
    }

    log.debug("Acquired lock for keys {} with token {} in store {}", context.keys, context.token, context.locked.storeId)

    scheduleLockRefresh(context, expiration)
    return invocation.proceed()
  }

  private fun scheduleLockRefresh(context: LockContext, expiration: Long) {
    val refresh = intervalConverter.toMillis(context.locked.refresh)
    if (refresh > 0 && taskScheduler != null) {
      val startTime = Date.from(Instant.now().plusMillis(refresh))
      context.scheduledFuture = taskScheduler.scheduleAtFixedRate(
        { context.lock.refresh(context.keys, context.locked.storeId, context.token!!, expiration) },
        startTime,
        refresh
      )
    }
  }

  private fun cleanAfterExecution(context: LockContext) {
    val scheduledFuture = context.scheduledFuture
    if (scheduledFuture != null && !scheduledFuture.isCancelled && !scheduledFuture.isDone) {
      scheduledFuture.cancel(true)
    }

    if (!context.token.isNullOrEmpty() && !context.locked.manuallyReleased) {
      val released = context.lock.release(context.keys, context.locked.storeId, context.token!!)
      if (released) {
        log.debug("Released lock for keys {} with token {} in store {}", context.keys, context.token, context.locked.storeId)
      } else {
        log.error("Couldn't release lock for keys {} with token {} in store {}", context.keys, context.token, context.locked.storeId)
      }
    }
  }

  private inner class LockContext(invocation: MethodInvocation) {
    val method = AopUtils.getMostSpecificMethod(invocation.method, invocation.`this`!!.javaClass)
    val locked: Locked = AnnotatedElementUtils.findMergedAnnotation(method, Locked::class.java)!!
    val lock: Lock = lockTypeResolver.get(locked.type.java)
    val keys: List<String> = resolveKeys(invocation)

    var token: String? = null
    var scheduledFuture: ScheduledFuture<*>? = null

    init {
      validateConstructedContext()
    }

    private fun resolveKeys(invocation: MethodInvocation): List<String> {
      try {
        return keyGenerator.resolveKeys(locked.prefix, locked.expression, invocation.`this`!!, method, invocation.arguments)
      } catch (e: RuntimeException) {
        throw DistributedLockException("Cannot resolve keys to lock: $locked on method $method", e)
      }
    }

    private fun validateConstructedContext() {
      if (locked.expression.isEmpty()) {
        throw DistributedLockException("Missing expression: $locked on method $method")
      }
    }
  }
}
