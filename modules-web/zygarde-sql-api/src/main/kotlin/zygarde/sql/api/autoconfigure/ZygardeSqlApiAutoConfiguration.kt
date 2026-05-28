package zygarde.sql.api.autoconfigure

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import zygarde.sql.api.exception.SqlDataAccessExceptionMapper

@Configuration(proxyBeanMethods = false)
class ZygardeSqlApiAutoConfiguration {
  @ConditionalOnMissingBean
  @Bean
  fun sqlDataAccessExceptionMapper() = SqlDataAccessExceptionMapper()
}
