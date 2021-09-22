package zygarde.test.extension

import zygarde.core.exception.BusinessException
import zygarde.core.exception.ErrorCode
import io.kotest.assertions.failure
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe

fun errCodeMatches(errorCode: ErrorCode, block: () -> Unit) {
  val businessException = shouldThrow<BusinessException> { block() }
  if (businessException.code != errorCode) {
    throw failure("Expected errorCode $errorCode but got ${businessException.code}")
  }
}

fun errMessageMatches(message: String, block: () -> Unit) {
  shouldThrow<BusinessException> { block() }.message shouldBe message
}
