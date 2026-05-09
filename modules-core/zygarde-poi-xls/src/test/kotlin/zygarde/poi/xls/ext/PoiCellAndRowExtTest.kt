package zygarde.poi.xls.ext

import io.kotest.matchers.shouldBe
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.jupiter.api.Test
import zygarde.poi.xls.ext.PoiCellExt.getCellNumber
import zygarde.poi.xls.ext.PoiCellExt.getString
import zygarde.poi.xls.ext.PoiRowExt.getCellNumber
import zygarde.poi.xls.ext.PoiRowExt.getCellString
import java.math.BigDecimal
import java.math.RoundingMode

class PoiCellAndRowExtTest {
  @Test
  fun `cell helpers should read string numeric and unsupported cells`() {
    XSSFWorkbook().use { workbook ->
      val row = workbook.createSheet("s").createRow(0)
      val stringCell = row.createCell(0).also { it.setCellValue("42.50") }
      val numericCell = row.createCell(1).also { it.setCellValue(12.345) }
      val blankCell = row.createCell(2, CellType.BLANK)

      stringCell.getString() shouldBe "42.50"
      stringCell.getCellNumber() shouldBe BigDecimal("42.50")
      numericCell.getString(scaleWhenNumeric = 2, scaleRoundingWhenNumeric = RoundingMode.HALF_UP) shouldBe "12.35"
      numericCell.getCellNumber() shouldBe BigDecimal.valueOf(12.345)
      blankCell.getString() shouldBe null
      blankCell.getCellNumber() shouldBe null
    }
  }

  @Test
  fun `row helpers should resolve numeric and alphabetic columns`() {
    XSSFWorkbook().use { workbook ->
      val row = workbook.createSheet("s").createRow(0)
      row.createCell(0).setCellValue("alpha")
      row.createCell(1).setCellValue(7.0)

      row.getCellString("A") shouldBe "alpha"
      row.getCellString(1) shouldBe "7"
      row.getCellNumber("B") shouldBe BigDecimal.valueOf(7.0)
      row.getCellNumber(2) shouldBe null
    }
  }
}
