package zygarde.fixture.props

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties("zygarde.fixture")
class ZygardeFixtureProperties {
  var enabled: Boolean = true
}
