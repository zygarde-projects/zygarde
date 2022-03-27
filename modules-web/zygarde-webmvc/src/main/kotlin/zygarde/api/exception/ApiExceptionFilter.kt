package zygarde.api.exception

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.filter.OncePerRequestFilter
import zygarde.api.tracing.ApiTracingContext
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * @author leo
 */
class ApiExceptionFilter(
  @Autowired val apiExceptionHandler: ApiExceptionHandler,
  @Autowired val objectMapper: ObjectMapper
) : OncePerRequestFilter() {

  override fun doFilterInternal(req: HttpServletRequest, res: HttpServletResponse, chain: FilterChain) {
    ApiTracingContext.trace(req) {
      try {
        chain.doFilter(req, res)
      } catch (t: Throwable) {
        val responseEntity = apiExceptionHandler.handleThrowable(t, req)
        res.status = responseEntity.statusCodeValue
        res.contentType = "application/json;charset=UTF-8"
        res.writer.write(objectMapper.writeValueAsString(responseEntity.body))
      }
    }
  }
}
