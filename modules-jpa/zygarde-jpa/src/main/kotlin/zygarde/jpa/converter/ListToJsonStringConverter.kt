package zygarde.jpa.converter

import zygarde.json.jsonStringToList
import zygarde.json.toJsonString
import java.io.Serializable
import jakarta.persistence.AttributeConverter
import kotlin.reflect.KClass

abstract class ListToJsonStringConverter<T : Serializable>(val clz: KClass<T>) : AttributeConverter<Collection<T>, String> {
  override fun convertToDatabaseColumn(attribute: Collection<T>?): String {
    return (attribute ?: emptyList<T>()).toJsonString()
  }

  override fun convertToEntityAttribute(dbData: String?): Collection<T> {
    return dbData?.jsonStringToList(clz) ?: emptyList<T>()
  }
}
