package zygarde.test.api

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import zygarde.api.tracing.ApiTracingContext

@RestController
class TestApi {

  @GetMapping("/apiId")
  fun getApiId(): String {
    return ApiTracingContext.getTracingData().apiId
  }
}
