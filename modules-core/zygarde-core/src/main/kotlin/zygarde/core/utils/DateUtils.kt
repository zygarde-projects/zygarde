package zygarde.core.utils

import zygarde.core.extension.date.isBetween
import java.time.LocalDateTime

object DateUtils {

  fun overlap(
    srcStart: LocalDateTime,
    srcEnd: LocalDateTime,
    targetStart: LocalDateTime,
    targetEnd: LocalDateTime,
  ): Boolean {
    if (srcStart.isBetween(targetStart, targetEnd)) {
      return true
    }
    if (srcEnd.isBetween(targetStart, targetEnd)) {
      return true
    }
    if (targetStart.isBetween(srcStart, srcEnd)) {
      return true
    }
    if (targetEnd.isBetween(srcStart, srcEnd)) {
      return true
    }
    return false
  }
}
