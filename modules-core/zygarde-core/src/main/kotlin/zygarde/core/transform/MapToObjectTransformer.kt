package zygarde.core.transform

import java.lang.reflect.Proxy
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.javaType

class MapToObjectTransformer<T : Any>(
  private val clz: KClass<T>
) {

  private val propertyMap = clz.memberProperties.associateBy { it.name }
  private val propertyGetterToNameMap = clz.memberProperties.associate { it.javaGetter to it.name }

  @Suppress("UNCHECKED_CAST")
  fun transform(map: Map<String, Any?>): T {
    return if (clz.isAbstract) {
      // Handle the interface case
      return Proxy.newProxyInstance(clz.java.classLoader, arrayOf(clz.java)) { _, method, _ ->
        val value = propertyGetterToNameMap[method]?.let { map[it] }
        value.resolveByType(method.returnType)
      } as T
    } else {
      // Handle the class case
      val primaryConstructor = clz.primaryConstructor
      val excludeSetterPropNames = mutableListOf<String>()
      val instance = if (primaryConstructor != null) {
        primaryConstructor.isAccessible = true
        val primaryConstructorArgMap = primaryConstructor.parameters.associateWith {
          val value = map[it.name]
          value.resolveByType(it.type.javaType as Class<*>)
        }
        excludeSetterPropNames.addAll(primaryConstructorArgMap.keys.mapNotNull { it.name })
        primaryConstructor.callBy(primaryConstructorArgMap)
      } else {
        clz.createInstance()
      }
      propertyMap.filterKeys { !excludeSetterPropNames.contains(it) }.forEach { (name, prop) ->
        prop.applyValue(instance, map[name].resolveByType(prop.returnType.javaType as Class<*>))
      }
      instance
    }
  }

  private fun Any?.resolveByType(targetType: Class<*>): Any? {
    return if (targetType.isEnum) {
      targetType.enumConstants.find { (it as Enum<*>).name == this }
    } else {
      this
    }
  }

  private fun KProperty1<*, *>.applyValue(instance: Any, value: Any?) {
    this.javaField?.let {
      it.isAccessible = true
      it.set(instance, value)
    }
  }
}
