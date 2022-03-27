package zygarde.core.autoconfigure

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import zygarde.api.exception.ApiExceptionHandler
import zygarde.api.exception.filter.AuthExceptionFilter
import zygarde.api.exception.mapper.AccessDeniedExceptionMapper

@Configuration
class ZygardeSpringSecurityConfig {

  @ConditionalOnMissingBean
  @Bean
  fun accessDeniedExceptionMapper() = AccessDeniedExceptionMapper()

  @ConditionalOnMissingBean
  @Bean
  fun authExceptionFilter(
    apiExceptionHandler: ApiExceptionHandler,
    objectMapper: ObjectMapper,
  ) = AuthExceptionFilter(apiExceptionHandler, objectMapper)
}
