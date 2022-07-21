package zygarde.core.extension.date

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

fun LocalDate.toDate(zoneId: ZoneId = ZoneId.systemDefault()): Date {
  return this.atStartOfDay().toDate(zoneId)
}

fun LocalDateTime.toDate(zoneId: ZoneId = ZoneId.systemDefault()): Date {
  return Date.from(this.atZone(zoneId).toInstant())
}

fun LocalDate.isBetween(from: LocalDate, to: LocalDate, includingFrom: Boolean = true, includingTo: Boolean = true): Boolean {
  if (this.isAfter(from) && this.isBefore(to)) {
    return true
  }
  return (includingFrom && this.isEqual(from)) || (includingTo && this.isEqual(to))
}

fun LocalDateTime.isBetween(from: LocalDateTime, to: LocalDateTime, includingFrom: Boolean = true, includingTo: Boolean = true): Boolean {
  if (this.isAfter(from) && this.isBefore(to)) {
    return true
  }
  return (includingFrom && this.isEqual(from)) || (includingTo && this.isEqual(to))
}
