package zygarde.security.filter

import org.springframework.web.filter.GenericFilterBean
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

/**
 * @author leo
 */
class CorsFilter(
  allowHeaders: List<String> = emptyList(),
  exposeHeaders: List<String> = emptyList(),
) : GenericFilterBean() {

  private val allowHeadersFlatted = allowHeaders.toSet().joinToString(", ")
  private val exposeHeadersFlatted = exposeHeaders.toSet().joinToString(", ")

  override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
    val req = request as HttpServletRequest
    val res = response as HttpServletResponse
    res.setHeader("Access-Control-Allow-Origin", req.getHeader("Origin"))
    res.setHeader("Access-Control-Allow-Credentials", "true")
    res.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, PUT, DELETE")
    res.setHeader("Access-Control-Max-Age", "3600")
    res.setHeader("Access-Control-Allow-Headers", "Content-Type, Accept, X-Requested-With, $allowHeadersFlatted")
    res.setHeader("Access-Control-Expose-Headers", exposeHeadersFlatted)
    if (req.method == "OPTIONS") {
      res.status = 200
    } else {
      chain.doFilter(request, response)
    }
  }
}
