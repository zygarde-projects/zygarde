package zygarde.fixture.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties("zygarde.fixture")
class ZygardeFixtureProperties {
  var enabled: Boolean = true
}
