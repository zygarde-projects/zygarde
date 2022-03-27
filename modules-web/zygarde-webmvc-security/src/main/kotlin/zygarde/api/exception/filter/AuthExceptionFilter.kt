package zygarde.api.exception.filter

import com.fasterxml.jackson.databind.ObjectMapper
import zygarde.api.exception.ApiExceptionFilter
import zygarde.api.exception.ApiExceptionHandler

class AuthExceptionFilter(
  apiExceptionHandler: ApiExceptionHandler,
  objectMapper: ObjectMapper
) : ApiExceptionFilter(apiExceptionHandler, objectMapper)
