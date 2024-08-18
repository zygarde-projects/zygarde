package zygarde.core.autoconfigure

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import zygarde.api.exception.mapper.AccessDeniedExceptionMapper

@Configuration
class ZygardeSpringSecurityConfig {

  @ConditionalOnMissingBean
  @Bean
  fun accessDeniedExceptionMapper() = AccessDeniedExceptionMapper()
}
