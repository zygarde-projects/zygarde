package zygarde.security.filter

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.util.AntPathMatcher
import org.springframework.web.filter.OncePerRequestFilter
import zygarde.core.exception.ApiErrorCode
import zygarde.core.exception.BusinessException
import zygarde.security.ApiRole
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

/**
 * @author leo
 */
class AuthFilter(
  val apiRoles: List<ApiRole>
) : OncePerRequestFilter() {

  val antPathMatcher = AntPathMatcher()

  override fun doFilterInternal(req: HttpServletRequest, res: HttpServletResponse, chain: FilterChain) {
    apiRoles.find { antPathMatcher.match(it.antPattern(), req.requestURI) }?.also { apiRole ->
      (req.getHeader(apiRole.authHeader()) ?: req.getHeader(apiRole.authHeader().lowercase()))?.also { authHeaderValue ->
        val authTokenService = apiRole.authTokenService()
        try {
          SecurityContextHolder.getContext().authentication = authTokenService?.auth(authHeaderValue)
        } catch (t: Throwable) {
          throw BusinessException(ApiErrorCode.UNAUTHORIZED, t.stackTraceToString())
        }
      }
    }

    chain.doFilter(req, res)
  }
}
