package zygarde.core.extension.date

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

fun LocalDate.toDate(zoneId: ZoneId = ZoneId.systemDefault()): Date {
  return this.atStartOfDay().toDate(zoneId)
}

fun LocalDateTime.toDate(zoneId: ZoneId = ZoneId.systemDefault()): Date {
  return Date.from(this.atZone(zoneId).toInstant())
}
