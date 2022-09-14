package zygarde.jpa.converter

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

abstract class LocalDateTimeConverter(formatter: DateTimeFormatter) : TemporalStringConverter<LocalDateTime>(formatter) {
  override fun convertToEntityAttribute(dbData: String?): LocalDateTime? {
    return dbData?.takeIf { it.isNotEmpty() }?.let { LocalDateTime.parse(it, formatter) }
  }
}
