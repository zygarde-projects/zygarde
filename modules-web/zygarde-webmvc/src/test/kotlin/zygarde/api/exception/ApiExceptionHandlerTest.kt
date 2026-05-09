package zygarde.api.exception

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import jakarta.servlet.FilterChain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.context.MessageSource
import org.springframework.context.NoSuchMessageException
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import zygarde.api.tracing.ApiTracingContext
import zygarde.core.exception.ApiErrorCode
import zygarde.core.exception.BusinessException
import zygarde.core.exception.ErrorCode
import java.util.Locale

class ApiExceptionHandlerTest {
  private enum class TestErrorCode(
    override val code: String,
    override val message: String,
  ) : ErrorCode {
    DOMAIN_ERROR("D001", "Domain error"),
  }

  private class IllegalArgumentMapper : ExceptionToBusinessExceptionMapper<IllegalArgumentException>() {
    override fun supported(t: Throwable): Boolean = t is IllegalArgumentException

    override fun transform(t: IllegalArgumentException): BusinessException {
      return BusinessException(ApiErrorCode.BAD_REQUEST, "mapped ${t.message}")
    }
  }

  @AfterEach
  fun tearDown() {
    LocaleContextHolder.resetLocaleContext()
    ApiTracingContext.getTracingData().exception = null
  }

  private fun handler(
    messageSource: MessageSource = mockk(relaxed = true),
    mappers: List<ExceptionToBusinessExceptionMapper<*>> = emptyList()
  ): ApiExceptionHandler {
    return ApiExceptionHandler().also {
      ReflectionTestUtils.setField(it, "messageSource", messageSource)
      ReflectionTestUtils.setField(it, "exceptionToBusinessExceptionMappers", mappers)
    }
  }

  @Suppress("UNUSED_PARAMETER")
  private fun validationTarget(req: String) {
  }

  @Test
  fun `handleValidationError should translate field errors and fall back to default message`() {
    val locale = Locale.TAIWAN
    LocaleContextHolder.setLocale(locale)
    val messageSource = mockk<MessageSource>()
    every { messageSource.getMessage("known.code", any(), locale) } returns "translated message"
    every { messageSource.getMessage("missing.code", any(), locale) } throws NoSuchMessageException("missing.code")

    val bindingResult = BeanPropertyBindingResult(Any(), "createReq")
    bindingResult.addError(
      FieldError("createReq", "name", null, false, arrayOf("known.code"), emptyArray(), "must not be blank")
    )
    bindingResult.addError(
      FieldError("createReq", "description", null, false, arrayOf("missing.code"), emptyArray(), "must not be null")
    )
    val method = javaClass.getDeclaredMethod("validationTarget", String::class.java)
    val exception = MethodArgumentNotValidException(MethodParameter(method, 0), bindingResult)

    val response = handler(messageSource).handleValidationError(exception)

    response.statusCode shouldBe HttpStatus.BAD_REQUEST
    response.body?.code shouldBe ApiErrorCode.BAD_REQUEST.code
    response.body?.messages shouldContainExactly listOf(
      "translated message",
      "createReq.description must not be null"
    )
  }

  @Test
  fun `handleBusinessException should use HttpErrorCode status and collect messages`() {
    val cause = IllegalStateException("root cause")
    val exception = BusinessException(ApiErrorCode.NOT_FOUND, cause)

    val response = handler().handleBusinessException(exception, MockHttpServletRequest("GET", "/missing"))

    response.statusCode shouldBe HttpStatus.NOT_FOUND
    response.body?.code shouldBe ApiErrorCode.NOT_FOUND.code
    response.body?.messages shouldContainExactly listOf("Not found", "root cause")
    ApiTracingContext.getTracingData().exception shouldBe exception
  }

  @Test
  fun `handleBusinessException should fall back to expectation failed for non HTTP error codes`() {
    val exception = BusinessException(TestErrorCode.DOMAIN_ERROR)

    val response = handler().handleBusinessException(exception, MockHttpServletRequest("GET", "/domain"))

    response.statusCode shouldBe HttpStatus.EXPECTATION_FAILED
    response.body?.code shouldBe TestErrorCode.DOMAIN_ERROR.code
    response.body?.messages shouldContainExactly listOf("Domain error")
  }

  @Test
  fun `handleThrowable should unwrap causes and use business exception mappers`() {
    val request = MockHttpServletRequest("POST", "/api")
    val mapped = handler(mappers = listOf(IllegalArgumentMapper()))
      .handleThrowable(IllegalArgumentException("bad input"), request)
    val wrapped = handler()
      .handleThrowable(RuntimeException("wrapper", BusinessException(ApiErrorCode.BAD_REQUEST, "bad request")), request)

    mapped.statusCode shouldBe HttpStatus.BAD_REQUEST
    mapped.body?.messages shouldContainExactly listOf("mapped bad input")
    wrapped.statusCode shouldBe HttpStatus.BAD_REQUEST
    wrapped.body?.messages shouldContainExactly listOf("bad request")
  }

  @Test
  fun `handleThrowable should return server error when no mapper matches`() {
    val exception = IllegalStateException("boom")

    val response = handler().handleThrowable(exception, MockHttpServletRequest("GET", "/boom"))

    response.statusCode shouldBe HttpStatus.INTERNAL_SERVER_ERROR
    response.body?.code shouldBe ApiErrorCode.SERVER_ERROR.code
    response.body?.messages shouldContainExactly listOf("boom")
    ApiTracingContext.getTracingData().exception shouldBe exception
  }

  @Test
  fun `ApiExceptionFilter should write handler response as JSON`() {
    val filter = ApiExceptionFilter(handler(), ObjectMapper())
    val request = MockHttpServletRequest("GET", "/filtered")
    val response = MockHttpServletResponse()
    val chain = FilterChain { _, _ ->
      throw BusinessException(ApiErrorCode.UNAUTHORIZED)
    }

    filter.doFilter(request, response, chain)

    response.status shouldBe HttpStatus.UNAUTHORIZED.value()
    response.contentType shouldBe "application/json;charset=UTF-8"
    response.contentAsString shouldBe """{"code":"401","name":"UNAUTHORIZED","messages":["Unauthorized"]}"""
  }
}
