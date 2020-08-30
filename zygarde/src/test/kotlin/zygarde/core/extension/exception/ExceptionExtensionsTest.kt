package puni.extension.exception

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import zygarde.core.exception.CommonErrorCode
import zygarde.core.extension.exception.errWhen
import zygarde.core.extension.exception.errWhenException
import zygarde.core.extension.exception.errWhenNull
import zygarde.core.extension.exception.getStackTraceString
import zygarde.core.extension.exception.nullWhenError
import zygarde.test.extension.errCodeMatches
import zygarde.test.extension.errMessageMatches
import java.lang.RuntimeException

class ExceptionExtensionsTest : StringSpec(
  {
    "should able to catch errorCode" {
      errCodeMatches(CommonErrorCode.ERROR) {
        1.errWhen(CommonErrorCode.ERROR) { it == 1 }
      }
    }
    "should pass expression" {
      1.errWhen(CommonErrorCode.ERROR) { it != 1 }
    }
    "should able to catch errWhenNull" {
      errCodeMatches(CommonErrorCode.ERROR) {
        val foo: Int? = null
        foo.errWhenNull(CommonErrorCode.ERROR)
      }
      "bar".errWhenNull(CommonErrorCode.ERROR)
    }
    "should able to catch exception" {
      1.errWhenException(CommonErrorCode.ERROR) {
        it.toString()
      } shouldBe "1"
      errCodeMatches(CommonErrorCode.ERROR) {
        1.errWhenException(CommonErrorCode.ERROR) {
          throw Exception()
        }
      }
    }
    "should able to handle with message" {
      errMessageMatches("1+2=3") {
        1.errWhen(CommonErrorCode.ERROR, "{}+{}={}", 1, 2, 3) { it == 1 }
      }
      errMessageMatches("foo") {
        1.errWhen(CommonErrorCode.ERROR, "foo") { it == 1 }
      }
    }
    "should able to get null when error" {
      nullWhenError { Class.forName("not_exist") } shouldBe null
      nullWhenError { "foo" } shouldBe "foo"
    }
    "should able to getStackTraceString" {
      var s = ""
      try {
        throw RuntimeException("test")
      } catch (e: Throwable) {
        s = e.getStackTraceString()
      }
      s shouldNotBe ""
    }
  }
)
