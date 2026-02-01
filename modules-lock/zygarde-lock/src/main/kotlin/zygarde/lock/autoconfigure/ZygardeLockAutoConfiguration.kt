package zygarde.lock.autoconfigure

import zygarde.lock.Lock
import zygarde.lock.advice.LockBeanPostProcessor
import zygarde.lock.advice.LockTypeResolver
import zygarde.lock.exception.DistributedLockException
import zygarde.lock.interval.BeanFactoryAwareIntervalConverter
import zygarde.lock.interval.IntervalConverter
import zygarde.lock.key.KeyGenerator
import zygarde.lock.key.SpelKeyGenerator
import zygarde.lock.retry.DefaultRetriableLockFactory
import zygarde.lock.retry.DefaultRetryTemplateConverter
import zygarde.lock.retry.RetriableLockFactory
import org.springframework.beans.factory.NoUniqueBeanDefinitionException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.core.convert.ConversionService
import org.springframework.core.convert.support.DefaultConversionService
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.scheduling.config.TaskManagementConfigUtils

@Configuration
open class ZygardeLockAutoConfiguration {
  companion object {
    @Bean
    @ConditionalOnMissingBean
    @JvmStatic
    fun lockBeanPostProcessor(
      @Lazy keyGenerator: KeyGenerator,
      @Lazy lockTypeResolver: LockTypeResolver,
      @Lazy intervalConverter: IntervalConverter,
      @Lazy retriableLockFactory: RetriableLockFactory,
      @Lazy @Autowired(required = false) distributedLockTaskScheduler: TaskScheduler?
    ): LockBeanPostProcessor {
      return LockBeanPostProcessor(keyGenerator, lockTypeResolver, intervalConverter, retriableLockFactory, distributedLockTaskScheduler).apply {
        setBeforeExistingAdvisors(true)
      }
    }
  }

  @Bean
  @ConditionalOnMissingBean
  open fun intervalConverter(
    @Lazy configurableBeanFactory: ConfigurableBeanFactory
  ): IntervalConverter {
    return BeanFactoryAwareIntervalConverter(configurableBeanFactory)
  }

  @Bean
  @ConditionalOnMissingBean
  open fun retriableLockFactory(
    @Lazy intervalConverter: IntervalConverter
  ): RetriableLockFactory {
    return DefaultRetriableLockFactory(DefaultRetryTemplateConverter(intervalConverter))
  }

  @Bean
  @ConditionalOnMissingBean
  open fun spelKeyGenerator(
    @Lazy @Autowired(required = false) conversionService: ConversionService?
  ): KeyGenerator {
    return SpelKeyGenerator(conversionService ?: DefaultConversionService.getSharedInstance())
  }

  @Bean
  @ConditionalOnMissingBean
  open fun lockTypeResolver(
    @Lazy configurableBeanFactory: ConfigurableBeanFactory
  ): LockTypeResolver {
    return LockTypeResolver { type ->
      try {
        configurableBeanFactory.getBean(type)
      } catch (e: NoUniqueBeanDefinitionException) {
        if (type == Lock::class.java) {
          throw DistributedLockException(
            "Multiple Lock beans found and no specific type was set in @Locked. " +
              "Use a concrete annotation like @RedisLocked/@RedisMultiLocked or set type explicitly.",
            e
          )
        }
        throw e
      }
    }
  }

  @Bean
  @ConditionalOnMissingBean(name = [TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME])
  @ConditionalOnProperty(prefix = "zygarde.lock.task-scheduler.default", name = ["enabled"], havingValue = "true", matchIfMissing = true)
  open fun distributedLockTaskScheduler(): TaskScheduler {
    return ThreadPoolTaskScheduler()
  }
}
