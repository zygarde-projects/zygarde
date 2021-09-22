package zygarde.fixture.autoconfigure

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import zygarde.fixture.FixtureRunner

@Configuration
class ZygardeSpringFixtureConfig {

  @Bean
  @ConditionalOnProperty("zygarde.fixture.enabled", havingValue = "true")
  @ConditionalOnMissingBean
  fun fixtureRunner(applicationContext: ApplicationContext): FixtureRunner = FixtureRunner(applicationContext)
}
