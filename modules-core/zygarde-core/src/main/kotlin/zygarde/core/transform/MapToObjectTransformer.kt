package zygarde.core.transform

import java.lang.reflect.Proxy
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaGetter

class MapToObjectTransformer<T : Any>(
  private val clz: KClass<T>
) {

  private val propertyMap = clz.memberProperties.associateBy { it.name }

  @Suppress("UNCHECKED_CAST")
  fun transform(map: Map<String, Any?>): T {

    return if (clz.isAbstract) {
      // Handle the interface case
      return Proxy.newProxyInstance(clz.java.classLoader, arrayOf(clz.java)) { _, method, _ ->
        map[propertyMap.entries.find { it.value.javaGetter == method }?.key]
      } as T
    } else {
      // Handle the class case
      val primaryConstructor = clz.primaryConstructor
      if (primaryConstructor != null) {
        primaryConstructor.isAccessible = true
        val primaryConstructorArgMap = primaryConstructor.parameters.associateWith { map[it.name] }
        val primaryConstructorArgNames = primaryConstructorArgMap.keys.mapNotNull { it.name }
        val instance = primaryConstructor.callBy(primaryConstructorArgMap)
        propertyMap.forEach { (name, prop) ->
          if (prop is KMutableProperty1 && !primaryConstructorArgNames.contains(name)) {
            prop.isAccessible = true
            prop.setter.call(instance, map[name])
          }
        }
        instance
      } else {
        val instance = clz.createInstance()
        propertyMap.forEach { (name, prop) ->
          if (map[name] != null && prop is KMutableProperty1) {
            prop.isAccessible = true
            prop.setter.call(instance, map[name])
          }
        }
        instance
      }
    }
  }
}
