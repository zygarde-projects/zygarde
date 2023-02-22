package zygarde.api.exception

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.filter.GenericFilterBean
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

/**
 * @author leo
 */
@Component
open class ApiExceptionFilter(
  @Autowired val apiExceptionHandler: ApiExceptionHandler,
  @Autowired val objectMapper: ObjectMapper
) : GenericFilterBean() {

  override fun doFilter(req: ServletRequest, res: ServletResponse, chain: FilterChain) {
    try {
      chain.doFilter(req, res)
    } catch (t: Throwable) {
      val responseEntity = apiExceptionHandler.handleThrowable(t, req as HttpServletRequest)
      (res as HttpServletResponse).also {
        it.status = responseEntity.statusCodeValue
        it.contentType = "application/json;charset=UTF-8"
        it.writer.write(objectMapper.writeValueAsString(responseEntity.body))
      }
    }
  }
}
