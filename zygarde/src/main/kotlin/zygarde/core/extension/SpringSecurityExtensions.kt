package zygarde.core.extension

import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder

object SpringSecurityExtensions {
  inline fun <reified T> currentAuthenticationDetail(): T = SecurityContextHolder.getContext().authentication.let {
    if (it.isAuthenticated && it.details is T) {
      return it.details as T
    }
    throw AccessDeniedException("not logged in")
  }
}
