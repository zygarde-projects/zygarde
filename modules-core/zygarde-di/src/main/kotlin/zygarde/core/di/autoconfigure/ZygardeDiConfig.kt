package zygarde.core.di.autoconfigure

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import zygarde.core.di.DiServiceContext

@Configuration
class ZygardeDiConfig {
  @Bean
  fun diServiceContext() = DiServiceContext
}
