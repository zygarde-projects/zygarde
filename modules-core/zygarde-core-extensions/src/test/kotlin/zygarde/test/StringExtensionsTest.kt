package zygarde.test

import io.kotest.matchers.shouldBe
import org.apache.commons.io.IOUtils
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import zygarde.core.extension.string.StringSqlExtensions.shortenSelectSql
import zygarde.core.extension.string.isNumeric
import zygarde.core.extension.string.replaceByArgs

/**
 * @author leo
 */
class StringExtensionsTest {
  @Test
  fun `should able to replace {} in string  to arg`() {
    "this is a {}".replaceByArgs("book") shouldBe "this is a book"
    "this is a {} {}".replaceByArgs("book") shouldBe "this is a book {}"
    "{} {} {}".replaceByArgs("I", "am", "Groot") shouldBe "I am Groot"
    "this is an apple".replaceByArgs("book") shouldBe "this is an apple"
  }

  @Test
  fun `should able to check if string is numeric`() {
    "123".isNumeric() shouldBe true
    "-0223".isNumeric() shouldBe true
    "5.8".isNumeric() shouldBe true
    "a".isNumeric() shouldBe false
  }

  @Test
  fun `should able to shorten complex select sql`() {
    val raw = ClassPathResource("data/sql/complex_query_oracle.sql").inputStream.use { IOUtils.toString(it, "UTF-8") }
    val shorten = ClassPathResource("data/sql/complex_query_oracle_shorten.sql").inputStream.use { IOUtils.toString(it, "UTF-8") }
      .replace("\\n|\\r\\n".toRegex(), "")
    raw.shortenSelectSql() shouldBe shorten
  }

  @Test
  fun `should able to shorten select count sql`() {
    val raw = ClassPathResource("data/sql/count_query.sql").inputStream.use { IOUtils.toString(it, "UTF-8") }
    val shorten = ClassPathResource("data/sql/count_query_shorten.sql").inputStream.use { IOUtils.toString(it, "UTF-8") }
      .replace("\\n|\\r\\n".toRegex(), "")
    raw.shortenSelectSql() shouldBe shorten
  }
}
