package zygarde.core.autoconfigure

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import zygarde.api.exception.ApiErrorResponseFactory
import zygarde.api.exception.ApiExceptionHandler
import zygarde.api.exception.ApiExceptionResolver
import zygarde.api.exception.DefaultApiErrorResponseFactory
import zygarde.api.exception.mapper.KotlinInvalidNullExceptionMapper
import zygarde.api.openapi.SortableFieldsOperationCustomizer
import zygarde.api.tracing.ApiTracingFilter
import zygarde.api.tracing.ApiTracingHandlerInterceptor
import zygarde.json.JacksonCommon

@Configuration
class ZygardeSpringWebmvcConfig : WebMvcConfigurer {
  @Bean
  fun apiTracingHandlerInterceptor() = ApiTracingHandlerInterceptor()

  @Bean
  @ConditionalOnMissingBean
  fun sortableFieldsOperationCustomizer() = SortableFieldsOperationCustomizer()

  @ConditionalOnMissingBean
  @Bean
  fun kotlinInvalidNullExceptionMapper() = KotlinInvalidNullExceptionMapper()

  @Bean
  @ConditionalOnMissingBean(ApiExceptionResolver::class)
  fun apiExceptionHandler(): ApiExceptionHandler = ApiExceptionHandler()

  @Bean
  @ConditionalOnMissingBean
  fun apiErrorResponseFactory(): ApiErrorResponseFactory = DefaultApiErrorResponseFactory()

  @ConditionalOnMissingBean
  @Bean
  fun objectMapper(): ObjectMapper = JacksonCommon.objectMapper()

  @Order(Ordered.HIGHEST_PRECEDENCE)
  @Bean
  fun apiTracingFilter(): ApiTracingFilter = ApiTracingFilter()

  override fun addInterceptors(registry: InterceptorRegistry) {
    registry.addInterceptor(apiTracingHandlerInterceptor())
    super.addInterceptors(registry)
  }
}
