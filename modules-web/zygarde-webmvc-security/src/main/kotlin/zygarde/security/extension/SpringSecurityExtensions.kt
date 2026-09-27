package zygarde.security.extension

import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder

object SpringSecurityExtensions {
  inline fun <reified T> currentAuthenticationDetail(): T {
    val authentication = SecurityContextHolder.getContext().authentication
      ?: throw AccessDeniedException("not logged in")
    if (authentication.isAuthenticated && authentication.details is T) {
      return authentication.details as T
    }
    throw AccessDeniedException("not logged in")
  }
}
