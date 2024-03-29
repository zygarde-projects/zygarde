package zygarde.api.tracing

import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class ApiTracingHandlerInterceptor : HandlerInterceptor {

  override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
    if (handler is HandlerMethod) {
      ApiTracingContext.setApiId(
        "${handler.beanType.simpleName}.${handler.method.name}"
      )
    }
    return super.preHandle(request, response, handler)
  }
}
