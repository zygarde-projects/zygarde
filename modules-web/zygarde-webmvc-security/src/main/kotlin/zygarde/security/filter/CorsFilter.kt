package zygarde.security.filter

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.filter.GenericFilterBean
import zygarde.security.ApiRole
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * @author leo
 */
class CorsFilter(
  @Autowired val apiRoles: List<ApiRole>,
) : GenericFilterBean() {

  private val authHeaders by lazy { apiRoles.map { it.authHeader() }.joinToString(", ") }

  override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
    val req = request as HttpServletRequest
    val res = response as HttpServletResponse
    res.setHeader("Access-Control-Allow-Origin", req.getHeader("Origin"))
    res.setHeader("Access-Control-Allow-Credentials", "true")
    res.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, PUT, DELETE")
    res.setHeader("Access-Control-Max-Age", "3600")
    res.setHeader("Access-Control-Allow-Headers", "Content-Type, Accept, X-Requested-With, $authHeaders")
    res.setHeader("Access-Control-Expose-Headers", authHeaders)
    if (req.method == "OPTIONS") {
      res.status = 200
    } else {
      chain.doFilter(request, response)
    }
  }
}
