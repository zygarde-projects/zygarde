package zygarde.core.autoconfigure

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import zygarde.api.exception.ApiExceptionFilter
import zygarde.api.exception.ApiExceptionHandler
import zygarde.json.JacksonCommon

@Configuration
class ZygardeSpringWebmvcConfig {

  @Bean
  @ConditionalOnMissingBean
  fun apiExceptionHandler(): ApiExceptionHandler = ApiExceptionHandler()

  @ConditionalOnMissingBean
  @Bean
  fun objectMapper(): ObjectMapper = JacksonCommon.objectMapper()

  @Bean
  @ConditionalOnMissingBean
  fun apiExceptionFilter(
    apiExceptionHandler: ApiExceptionHandler,
    objectMapper: ObjectMapper
  ): ApiExceptionFilter = ApiExceptionFilter(apiExceptionHandler, objectMapper)
}
