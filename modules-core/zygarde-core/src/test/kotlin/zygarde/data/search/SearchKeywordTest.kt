package zygarde.data.search

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SearchKeywordTest {
  @Test
  fun `should create SearchKeyword with default values`() {
    // given & when
    val search = SearchKeyword()

    // then
    search.keyword shouldBe null
    search.type shouldBe SearchKeywordType.STARTS_WITH
  }

  @Test
  fun `should create SearchKeyword with keyword`() {
    // given & when
    val search = SearchKeyword(
      keyword = "test"
    )

    // then
    search.keyword shouldBe "test"
    search.type shouldBe SearchKeywordType.STARTS_WITH
  }

  @Test
  fun `should create SearchKeyword with all fields`() {
    // given & when
    val search = SearchKeyword(
      keyword = "example",
      type = SearchKeywordType.CONTAINS
    )

    // then
    search.keyword shouldBe "example"
    search.type shouldBe SearchKeywordType.CONTAINS
  }

  @Test
  fun `should support all search types`() {
    val types = listOf(
      SearchKeywordType.STARTS_WITH,
      SearchKeywordType.ENDS_WITH,
      SearchKeywordType.CONTAINS,
      SearchKeywordType.MATCH
    )

    types.forEach { type ->
      val search = SearchKeyword(keyword = "test", type = type)
      search.type shouldBe type
    }
  }
}
