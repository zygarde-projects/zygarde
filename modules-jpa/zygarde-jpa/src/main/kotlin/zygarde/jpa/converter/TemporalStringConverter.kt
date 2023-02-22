package zygarde.jpa.converter

import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor
import jakarta.persistence.AttributeConverter

abstract class TemporalStringConverter<T : TemporalAccessor>(val formatter: DateTimeFormatter) : AttributeConverter<T?, String> {
  override fun convertToDatabaseColumn(attribute: T?): String? {
    return attribute?.let(formatter::format)
  }
}
