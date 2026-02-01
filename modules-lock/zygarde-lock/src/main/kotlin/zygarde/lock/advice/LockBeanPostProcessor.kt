package zygarde.lock.advice

import zygarde.lock.Locked
import zygarde.lock.interval.IntervalConverter
import zygarde.lock.key.KeyGenerator
import zygarde.lock.retry.RetriableLockFactory
import org.springframework.aop.framework.AbstractAdvisingBeanPostProcessor
import org.springframework.aop.support.DefaultPointcutAdvisor
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut
import org.springframework.beans.factory.InitializingBean
import org.springframework.scheduling.TaskScheduler

class LockBeanPostProcessor(
  private val keyGenerator: KeyGenerator,
  private val lockTypeResolver: LockTypeResolver,
  private val intervalConverter: IntervalConverter,
  private val retriableLockFactory: RetriableLockFactory,
  private val taskScheduler: TaskScheduler?
) : AbstractAdvisingBeanPostProcessor(), InitializingBean {
  override fun afterPropertiesSet() {
    val pointcut = AnnotationMatchingPointcut(null, Locked::class.java, true)
    val interceptor = LockMethodInterceptor(keyGenerator, lockTypeResolver, intervalConverter, retriableLockFactory, taskScheduler)
    this.advisor = DefaultPointcutAdvisor(pointcut, interceptor)
  }
}
