package zygarde.data.search

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SearchKeywordTypeTest {
  @Test
  fun `should have all keyword types`() {
    // given & when
    val values = SearchKeywordType.values()

    // then
    values.size shouldBe 4
    values shouldBe
      arrayOf(
        SearchKeywordType.STARTS_WITH,
        SearchKeywordType.ENDS_WITH,
        SearchKeywordType.CONTAINS,
        SearchKeywordType.MATCH,
      )
  }

  @Test
  fun `should parse from string`() {
    SearchKeywordType.valueOf("STARTS_WITH") shouldBe SearchKeywordType.STARTS_WITH
    SearchKeywordType.valueOf("ENDS_WITH") shouldBe SearchKeywordType.ENDS_WITH
    SearchKeywordType.valueOf("CONTAINS") shouldBe SearchKeywordType.CONTAINS
    SearchKeywordType.valueOf("MATCH") shouldBe SearchKeywordType.MATCH
  }
}
