package zygarde.core.extension.string

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * @author leo
 */
class StringExtensionsTest : StringSpec(
  {
    "should able to replace {} in string  to arg" {
      "this is a {}".replaceByArgs("book") shouldBe "this is a book"
      "this is a {} {}".replaceByArgs("book") shouldBe "this is a book {}"
      "{} {} {}".replaceByArgs("I", "am", "Groot") shouldBe "I am Groot"
      "this is an apple".replaceByArgs("book") shouldBe "this is an apple"
    }
    "should able to check if string is numeric" {
      "123".isNumeric() shouldBe true
      "-0223".isNumeric() shouldBe true
      "5.8".isNumeric() shouldBe true
      "a".isNumeric() shouldBe false
    }
  }
)
