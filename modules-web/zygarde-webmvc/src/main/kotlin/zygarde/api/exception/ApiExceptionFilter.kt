package zygarde.api.exception

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.filter.GenericFilterBean

/**
 * @author leo
 */
open class ApiExceptionFilter(
  @Autowired val apiExceptionResolver: ApiExceptionResolver,
  @Autowired val objectMapper: ObjectMapper
) : GenericFilterBean() {
  override fun doFilter(req: ServletRequest, res: ServletResponse, chain: FilterChain) {
    try {
      chain.doFilter(req, res)
    } catch (t: Throwable) {
      val responseEntity = apiExceptionResolver.handleThrowable(t, req as HttpServletRequest)
      (res as HttpServletResponse).also {
        it.status = responseEntity.statusCode.value()
        it.contentType = "application/json;charset=UTF-8"
        it.writer.write(objectMapper.writeValueAsString(responseEntity.body))
      }
    }
  }
}
