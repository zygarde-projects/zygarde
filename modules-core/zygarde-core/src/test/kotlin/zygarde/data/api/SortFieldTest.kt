package zygarde.data.api

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SortFieldTest {
  @Test
  fun `should create SortField with default values`() {
    // given & when
    val sortField = SortField()

    // then
    sortField.sort shouldBe null
    sortField.field shouldBe null
  }

  @Test
  fun `should create SortField with ascending sort`() {
    // given & when
    val sortField =
      SortField(
        sort = SortDirection.ASC,
        field = "name",
      )

    // then
    sortField.sort shouldBe SortDirection.ASC
    sortField.field shouldBe "name"
  }

  @Test
  fun `should create SortField with descending sort`() {
    // given & when
    val sortField =
      SortField(
        sort = SortDirection.DESC,
        field = "createdAt",
      )

    // then
    sortField.sort shouldBe SortDirection.DESC
    sortField.field shouldBe "createdAt"
  }

  @Test
  fun `should allow modifying values`() {
    // given
    val sortField = SortField()

    // when
    sortField.sort = SortDirection.ASC
    sortField.field = "updatedAt"

    // then
    sortField.sort shouldBe SortDirection.ASC
    sortField.field shouldBe "updatedAt"
  }
}
