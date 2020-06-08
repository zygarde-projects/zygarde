package zygarde.test.extension

import feign.FeignException
import io.kotest.assertions.failure
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.springframework.http.HttpStatus
import zygarde.core.exception.BusinessException
import zygarde.core.exception.ErrorCode
import zygarde.data.api.ApiErrorResponse
import zygarde.json.jsonStringToObject

/**
 * @author leo
 */
fun errCodeMatches(errorCode: ErrorCode, block: () -> Unit) {
  val businessException = shouldThrow<BusinessException> { block() }
  if (businessException.code != errorCode) {
    throw failure("Expected errorCode $errorCode but got ${businessException.code}")
  }
}

fun errMessageMatches(message: String, block: () -> Unit) {
  shouldThrow<BusinessException> { block() }.message shouldBe message
}

fun apiErrCodeMatches(errorCode: ErrorCode, block: () -> Unit) {
  val feignException = shouldThrow<FeignException> { block() }
  try {
    val json = feignException.contentUTF8()
    val apiErrorResponse = json.jsonStringToObject(ApiErrorResponse::class)
    if (apiErrorResponse.code != errorCode.code) {
      throw failure("Expected errorCode $errorCode(${errorCode.code}) but got ${apiErrorResponse.code}\r\nresponseBody=\r\n$json")
    }
  } catch (e: Exception) {
    throw failure("Error handling feignException, status=${feignException.status()}", e)
  }
}

fun httpStatusMatches(httpStatus: HttpStatus, block: () -> Unit) {
  val feignException = shouldThrow<FeignException> { block() }
  try {
    val responseBody = feignException.contentUTF8()
    if (feignException.status() != httpStatus.value()) {
      throw failure(
        """Expected httpStatus $httpStatus(${httpStatus.value()}) but got ${feignException.status()}
        |responseBody=
        |$responseBody""".trimMargin()
      )
    }
  } catch (e: Exception) {
    throw failure("Error handling feignException, status=${feignException.status()}", e)
  }
}
