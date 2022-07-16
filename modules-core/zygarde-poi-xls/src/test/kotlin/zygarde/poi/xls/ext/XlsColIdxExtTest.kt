package zygarde.poi.xls.ext

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import zygarde.poi.xls.ext.XlsColIdxExt.colIdxStr
import zygarde.poi.xls.ext.XlsColIdxExt.xlsColIdx

class XlsColIdxExtTest {
  @Test
  fun colIdxTrans() {
    listOf(
      "A" to 0,
      "B" to 1,
      "AA" to 26,
      "AB" to 27,
      "ZZ" to 701,
      "AAA" to 702,
    ).forEach {
      it.first.also { s ->
        s.xlsColIdx()
          .also { idx -> idx shouldBe it.second }
          .colIdxStr() shouldBe s
      }
    }
  }
}
