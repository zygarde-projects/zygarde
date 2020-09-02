package zygarde.test.feign

import feign.RequestInterceptor
import feign.RequestTemplate

/**
 * @author leo
 */
class TestUserRequestInterceptor : RequestInterceptor {
  override fun apply(template: RequestTemplate) {
    if (TestSupportContext.authTokenKey != null && TestSupportContext.authTokenValue != null) {
      template.header(TestSupportContext.authTokenKey, TestSupportContext.authTokenValue)
    }
  }
}
