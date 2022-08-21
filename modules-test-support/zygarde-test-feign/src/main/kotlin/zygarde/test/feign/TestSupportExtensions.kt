package zygarde.test.extension

import feign.FeignException
import io.kotest.assertions.failure
import io.kotest.assertions.throwables.shouldThrow
import org.springframework.http.HttpStatus
import zygarde.core.exception.ErrorCode
import zygarde.data.api.ApiErrorResponse
import zygarde.json.jsonStringToObject

/**
 * @author leo
 */
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

inline fun httpStatusMatches(httpStatus: HttpStatus, block: () -> Unit) {
  httpStatusMatches<Unit>(httpStatus, block)
}

@JvmName("httpStatusMatchesWithReturn")
inline fun <reified ERR_RES> httpStatusMatches(httpStatus: HttpStatus, block: () -> Unit): ERR_RES {
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
    if (ERR_RES::class == Unit::class) {
      return Unit as ERR_RES
    }
    return responseBody.jsonStringToObject()
  } catch (e: Exception) {
    throw failure("Error handling feignException, status=${feignException.status()}", e)
  }
}
