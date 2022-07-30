package zygarde.poi.xls.ext

import org.apache.poi.ss.usermodel.Row
import zygarde.poi.xls.ext.PoiCellExt.getCellNumber
import zygarde.poi.xls.ext.PoiCellExt.getString
import zygarde.poi.xls.ext.XlsColIdxExt.xlsColIdx
import java.math.BigDecimal
import java.math.RoundingMode

object PoiRowExt {

  fun Row.getCellString(
    col: String,
    scaleWhenNumeric: Int = 0,
    scaleRoundingWhenNumeric: RoundingMode = RoundingMode.HALF_UP,
  ): String? {
    return getCellString(col.xlsColIdx(), scaleWhenNumeric, scaleRoundingWhenNumeric)
  }

  fun Row.getCellString(
    colIdx: Int,
    scaleWhenNumeric: Int = 0,
    scaleRoundingWhenNumeric: RoundingMode = RoundingMode.HALF_UP,
  ): String? {
    return getCell(colIdx)?.getString(scaleWhenNumeric, scaleRoundingWhenNumeric)
  }

  fun Row.getCellNumber(col: String): BigDecimal? {
    return getCellNumber(col.xlsColIdx())
  }

  fun Row.getCellNumber(colIdx: Int): BigDecimal? {
    return getCell(colIdx)?.getCellNumber()
  }
}
