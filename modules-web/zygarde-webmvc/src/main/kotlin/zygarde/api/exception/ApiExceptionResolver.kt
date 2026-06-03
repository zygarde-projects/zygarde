package zygarde.api.exception

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity

interface ApiExceptionResolver {
  fun handleThrowable(t: Throwable, req: HttpServletRequest): ResponseEntity<Any>
}
