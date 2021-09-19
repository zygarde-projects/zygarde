package zygarde.test.api

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import zygarde.api.tracing.ApiTracingContext

@RestController
class TestApi(
  @Autowired val apiTracingContext: ApiTracingContext
) {

  @GetMapping("/apiId")
  fun getApiId(): String {
    return apiTracingContext.getTracingData().apiId
  }
}
