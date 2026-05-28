package zygarde.sql.api

import java.math.BigDecimal
import java.sql.Date
import java.sql.ResultSet
import java.sql.Time
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.UUID
import javax.sql.DataSource
import org.springframework.jdbc.datasource.DataSourceUtils

class ZygardeSqlExecutor(
  private val dataSource: DataSource,
) {
  fun <T> query(
    sql: String,
    params: Map<String, Any?> = emptyMap(),
    mapper: (ZygardeSqlRow) -> T
  ): List<T> {
    val parsedSql = NamedParameterSql.parse(sql)
    val connection = DataSourceUtils.getConnection(dataSource)
    try {
      connection.prepareStatement(parsedSql.sql).use { statement ->
        parsedSql.parameterNames.forEachIndexed { index, parameterName ->
          if (!params.containsKey(parameterName)) {
            throw IllegalArgumentException("Missing SQL parameter '$parameterName'")
          }
          statement.setObject(index + 1, params[parameterName])
        }
        statement.executeQuery().use { resultSet ->
          val result = mutableListOf<T>()
          while (resultSet.next()) {
            result.add(mapper(resultSet.toZygardeSqlRow()))
          }
          return result
        }
      }
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource)
    }
  }
}

class ZygardeSqlRow(
  values: Map<String, Any?>,
) {
  private val valuesByColumnName = values.mapKeys { it.key.lowercase() }

  inline fun <reified T> getRequired(name: String): T {
    val value = readValue(name)
    if (value == null) {
      throw IllegalStateException("Required SQL column '$name' was null")
    }
    return convertValue<T>(value)
  }

  inline fun <reified T> getNullable(name: String): T? {
    return readValue(name)?.let { convertValue<T>(it) }
  }

  @PublishedApi
  internal fun readValue(name: String): Any? {
    val normalizedName = name.lowercase()
    if (!valuesByColumnName.containsKey(normalizedName)) {
      throw IllegalArgumentException("SQL column '$name' was not found")
    }
    return valuesByColumnName[normalizedName]
  }
}

private fun ResultSet.toZygardeSqlRow(): ZygardeSqlRow {
  val metadata = metaData
  val values = (1..metadata.columnCount).associate { columnIndex ->
    metadata.getColumnLabel(columnIndex) to getObject(columnIndex)
  }
  return ZygardeSqlRow(values)
}

@PublishedApi
internal inline fun <reified T> convertValue(value: Any): T {
  val converted = when (T::class) {
    String::class -> value.toString()
    Int::class -> (value as Number).toInt()
    Long::class -> (value as Number).toLong()
    Double::class -> (value as Number).toDouble()
    BigDecimal::class -> value.toBigDecimal()
    Boolean::class -> value.toBoolean()
    LocalDate::class -> value.toLocalDate()
    LocalDateTime::class -> value.toLocalDateTime()
    OffsetDateTime::class -> value.toOffsetDateTime()
    LocalTime::class -> value.toLocalTime()
    UUID::class -> value.toUuid()
    else -> {
      if (value is T) {
        value
      } else {
        throw IllegalArgumentException("Unsupported SQL value conversion to ${T::class.qualifiedName}")
      }
    }
  }
  return converted as T
}

@PublishedApi
internal fun Any.toBigDecimal(): BigDecimal {
  return when (this) {
    is BigDecimal -> this
    is Number -> BigDecimal(toString())
    is String -> toBigDecimal()
    else -> throw IllegalArgumentException("Cannot convert ${this::class.qualifiedName} to BigDecimal")
  }
}

@PublishedApi
internal fun Any.toBoolean(): Boolean {
  return when (this) {
    is Boolean -> this
    is Number -> toInt() != 0
    is String -> toBooleanStrict()
    else -> throw IllegalArgumentException("Cannot convert ${this::class.qualifiedName} to Boolean")
  }
}

@PublishedApi
internal fun Any.toLocalDate(): LocalDate {
  return when (this) {
    is LocalDate -> this
    is Date -> toLocalDate()
    is Timestamp -> toLocalDateTime().toLocalDate()
    is String -> LocalDate.parse(this)
    else -> throw IllegalArgumentException("Cannot convert ${this::class.qualifiedName} to LocalDate")
  }
}

@PublishedApi
internal fun Any.toLocalDateTime(): LocalDateTime {
  return when (this) {
    is LocalDateTime -> this
    is Timestamp -> toLocalDateTime()
    is String -> LocalDateTime.parse(this)
    else -> throw IllegalArgumentException("Cannot convert ${this::class.qualifiedName} to LocalDateTime")
  }
}

@PublishedApi
internal fun Any.toOffsetDateTime(): OffsetDateTime {
  return when (this) {
    is OffsetDateTime -> this
    is String -> OffsetDateTime.parse(this)
    else -> throw IllegalArgumentException("Cannot convert ${this::class.qualifiedName} to OffsetDateTime")
  }
}

@PublishedApi
internal fun Any.toLocalTime(): LocalTime {
  return when (this) {
    is LocalTime -> this
    is Time -> toLocalTime()
    is String -> LocalTime.parse(this)
    else -> throw IllegalArgumentException("Cannot convert ${this::class.qualifiedName} to LocalTime")
  }
}

@PublishedApi
internal fun Any.toUuid(): UUID {
  return when (this) {
    is UUID -> this
    is String -> UUID.fromString(this)
    else -> throw IllegalArgumentException("Cannot convert ${this::class.qualifiedName} to UUID")
  }
}
