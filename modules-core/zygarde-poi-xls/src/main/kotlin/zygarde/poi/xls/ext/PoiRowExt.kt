package zygarde.poi.xls.ext

import org.apache.poi.ss.usermodel.Row
import zygarde.poi.xls.ext.PoiCellExt.getCellNumber
import zygarde.poi.xls.ext.PoiCellExt.getString
import zygarde.poi.xls.ext.XlsColIdxExt.xlsColIdx
import java.math.BigDecimal

object PoiRowExt {

  fun Row.getCellString(col: String, scaleWhenNumeric: Int = 0): String? {
    return getCellString(col.xlsColIdx(), scaleWhenNumeric)
  }

  fun Row.getCellString(colIdx: Int, scaleWhenNumeric: Int = 0): String? {
    return getCell(colIdx)?.getString(scaleWhenNumeric)
  }

  fun Row.getCellNumber(col: String): BigDecimal? {
    return getCellNumber(col.xlsColIdx())
  }

  fun Row.getCellNumber(colIdx: Int): BigDecimal? {
    return getCell(colIdx)?.getCellNumber()
  }
}
