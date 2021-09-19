package zygarde.api.tracing

import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class ApiTracingHandlerInterceptor(val apiTracingContext: ApiTracingContext) : HandlerInterceptorAdapter() {

  override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
    if (handler is HandlerMethod) {
      apiTracingContext.setApiId(
        "${handler.beanType.simpleName}.${handler.method.name}"
      )
    }

    return super.preHandle(request, response, handler)
  }
}