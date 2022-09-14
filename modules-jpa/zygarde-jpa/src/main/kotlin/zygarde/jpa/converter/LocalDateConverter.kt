package zygarde.jpa.converter

import java.time.LocalDate
import java.time.format.DateTimeFormatter

abstract class LocalDateConverter(formatter: DateTimeFormatter) : TemporalStringConverter<LocalDate>(formatter) {
  override fun convertToEntityAttribute(dbData: String?): LocalDate? {
    return dbData?.takeIf { it.isNotEmpty() }?.let { LocalDate.parse(it, formatter) }
  }
}
