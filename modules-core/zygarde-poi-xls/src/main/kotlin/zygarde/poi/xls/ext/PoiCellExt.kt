package zygarde.poi.xls.ext

import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import java.math.BigDecimal
import java.math.RoundingMode

object PoiCellExt {

  fun Cell.getString(
    scaleWhenNumeric: Int = 0,
    scaleRoundingWhenNumeric: RoundingMode = RoundingMode.HALF_UP,
  ): String? {
    return when (cellType) {
      CellType.STRING -> stringCellValue
      CellType.NUMERIC -> numericCellValue.toBigDecimal().setScale(scaleWhenNumeric, scaleRoundingWhenNumeric).toPlainString()
      else -> null
    }
  }

  fun Cell.getCellNumber(): BigDecimal? {
    return when (cellType) {
      CellType.STRING -> stringCellValue.toBigDecimalOrNull()
      CellType.NUMERIC -> numericCellValue.toBigDecimal()
      else -> null
    }
  }
}
