package zygarde.core.props

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties("zygarde.api")
class ZygardeApiProperties {
  var staticOptionApi = StaticOptionApiProp()

  class StaticOptionApiProp(
    var path: String = "/api/staticOptions"
  )
}
