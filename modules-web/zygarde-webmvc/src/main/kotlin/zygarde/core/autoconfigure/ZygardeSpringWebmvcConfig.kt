package zygarde.core.autoconfigure

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import zygarde.api.exception.ApiExceptionFilter
import zygarde.api.exception.ApiExceptionHandler
import zygarde.api.exception.mapper.MissingKotlinParameterExceptionMapper
import zygarde.api.tracing.ApiTracingContext
import zygarde.api.tracing.ApiTracingHandlerInterceptor
import zygarde.json.JacksonCommon

@Configuration
class ZygardeSpringWebmvcConfig : WebMvcConfigurer {

  @Bean
  fun apiTracingContext() = ApiTracingContext()

  @Bean
  fun apiTracingHandlerInterceptor() = ApiTracingHandlerInterceptor(apiTracingContext())

  @ConditionalOnMissingBean
  @Bean
  fun missingKotlinParameterExceptionMapper() = MissingKotlinParameterExceptionMapper()

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

  override fun addInterceptors(registry: InterceptorRegistry) {
    registry.addInterceptor(apiTracingHandlerInterceptor())
    super.addInterceptors(registry)
  }
}
