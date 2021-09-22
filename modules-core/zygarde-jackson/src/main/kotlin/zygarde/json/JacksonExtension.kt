package zygarde.json

import kotlin.reflect.KClass

fun Any?.toJsonString() = JacksonCommon.withObjectMapper { it.writeValueAsString(this) }

inline fun <reified T : Any> String.jsonStringToList(): List<T> = this.jsonStringToList(T::class)

fun <T : Any> String.jsonStringToList(clazz: KClass<T>): List<T> = JacksonCommon.withObjectMapper {
  it.readValue(this, it.typeFactory.constructCollectionType(List::class.java, clazz.java))
}

inline fun <reified K : Any, reified V : Any> String.jsonStringToMap(): Map<K, V?> = this.jsonStringToMap(K::class, V::class)

fun <K : Any, V : Any> String.jsonStringToMap(kClass: KClass<K>, vClass: KClass<V>): Map<K, V?> =
  JacksonCommon.withObjectMapper {
    it.readValue(this, it.typeFactory.constructMapType(Map::class.java, kClass.java, vClass.java))
  }

inline fun <reified T : Any> String.jsonStringToObject(): T = this.jsonStringToObject(T::class)

fun <T : Any> String.jsonStringToObject(clazz: KClass<T>): T = JacksonCommon.withObjectMapper {
  it.readValue(this, clazz.java)
}

inline fun <reified T : Any> Map<*, *>.jsonMapToObject(): T = this.toJsonString().jsonStringToObject(T::class)
