package zygarde.poi.xls.ext

object XlsColIdxExt {

  /**
   * A~Z 0~25
   * AA~AB 26~51
   * BA~BB 52~77
   */
  fun String.xlsColIdx(): Int {
    val charArray = this.uppercase().toCharArray()
    if (charArray.isEmpty()) {
      return charArray.first().code - 65
    }
    var resultIdx = 0
    charArray.forEachIndexed { idx, c ->
      if (idx < (charArray.size - 1)) {
        var pow = 26
        repeat(charArray.size - 2 - idx) { pow *= 26 }
        resultIdx += pow * (c.code - 64)
      } else {
        resultIdx += c.code - 65
      }
    }
    return resultIdx
  }

  fun Int.colIdxStr(): String {
    var tmp = this
    var resultStr = ""
    while (tmp >= 0) {
      val i = tmp % 26
      resultStr = (65 + i).toChar().toString() + resultStr
      tmp -= i
      if (tmp == 0) {
        break
      }
      tmp = (tmp / 26) - 1
    }
    return resultStr
  }
}
