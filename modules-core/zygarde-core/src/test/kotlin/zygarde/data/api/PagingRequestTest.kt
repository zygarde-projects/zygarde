package zygarde.data.api

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class PagingRequestTest {
  @Test
  fun `should create PagingRequest with default values`() {
    // given & when
    val request = PagingRequest()

    // then
    request.page shouldBe 1
    request.pageSize shouldBe 10
  }

  @Test
  fun `should create PagingRequest with custom values`() {
    // given & when
    val request = PagingRequest(
      page = 3,
      pageSize = 20
    )

    // then
    request.page shouldBe 3
    request.pageSize shouldBe 20
  }

  @Test
  fun `should allow modifying values`() {
    // given
    val request = PagingRequest()

    // when
    request.page = 5
    request.pageSize = 50

    // then
    request.page shouldBe 5
    request.pageSize shouldBe 50
  }
}
