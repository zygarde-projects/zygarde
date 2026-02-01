package zygarde.lock.redis.autoconfigure

import zygarde.lock.redis.impl.MultiRedisLock
import zygarde.lock.redis.impl.SimpleRedisLock
import java.util.UUID
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.StringRedisTemplate

@Configuration
@ConditionalOnClass(StringRedisTemplate::class)
open class ZygardeLockRedisAutoConfiguration {
  @Bean
  @ConditionalOnMissingBean
  open fun simpleRedisLock(stringRedisTemplate: StringRedisTemplate): SimpleRedisLock {
    return SimpleRedisLock({ UUID.randomUUID().toString() }, stringRedisTemplate)
  }

  @Bean
  @ConditionalOnMissingBean
  open fun multiRedisLock(stringRedisTemplate: StringRedisTemplate): MultiRedisLock {
    return MultiRedisLock(stringRedisTemplate)
  }
}
