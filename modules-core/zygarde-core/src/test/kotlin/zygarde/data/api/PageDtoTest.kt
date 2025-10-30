package zygarde.data.api

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class PageDtoTest {

  data class Item(val id: Int, val name: String)

  @Test
  fun `should create empty PageDto`() {
    // given & when
    val page = PageDto.empty<Item>()

    // then
    page.atPage shouldBe 0
    page.totalPages shouldBe 0
    page.items.shouldBeEmpty()
    page.totalCount shouldBe 0
  }

  @Test
  fun `should create PageDto with items`() {
    // given
    val items = listOf(
      Item(1, "Item 1"),
      Item(2, "Item 2"),
      Item(3, "Item 3")
    )

    // when
    val page = PageDto(
      atPage = 1,
      totalPages = 5,
      items = items,
      totalCount = 13
    )

    // then
    page.atPage shouldBe 1
    page.totalPages shouldBe 5
    page.items shouldBe items
    page.totalCount shouldBe 13
  }

  @Test
  fun `should support different item types`() {
    // given
    val stringPage = PageDto(
      atPage = 1,
      totalPages = 2,
      items = listOf("a", "b", "c"),
      totalCount = 5
    )

    val intPage = PageDto(
      atPage = 1,
      totalPages = 1,
      items = listOf(1, 2, 3),
      totalCount = 3
    )

    // then
    stringPage.items shouldBe listOf("a", "b", "c")
    intPage.items shouldBe listOf(1, 2, 3)
  }
}
