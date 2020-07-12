package zygarde.api.exception

import com.fasterxml.jackson.databind.ObjectMapper
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.GenericFilterBean

/**
 * @author leo
 */
@Component
@Order(Int.MIN_VALUE)
class ApiExceptionFilter(
  @Autowired val apiExceptionHandler: ApiExceptionHandler,
  @Autowired val objectMapper: ObjectMapper
) : GenericFilterBean() {

  override fun doFilter(req: ServletRequest, res: ServletResponse, chain: FilterChain) {
    try {
      chain.doFilter(req, res)
    } catch (t: Throwable) {
      val responseEntity = apiExceptionHandler.handleThrowable(t)
      (res as HttpServletResponse).also {
        it.status = responseEntity.statusCodeValue
        it.contentType = "application/json;charset=UTF-8"
        it.writer.write(objectMapper.writeValueAsString(responseEntity.body))
      }
    }
  }
}
