package example

import example.ApiHelper.apiToFeignClientClassMapping
import io.github.classgraph.ClassGraph
import org.springframework.cloud.openfeign.FeignClient
import zygarde.core.di.DiServiceContext
import kotlin.reflect.KClass

object ApiHelper {
  val apiToFeignClientClassMapping: MutableMap<KClass<*>, KClass<*>> = mutableMapOf()

  init {
    val classes = ClassGraph()
      .enableClassInfo()
      .enableAnnotationInfo()
      .scan()
      .allClasses
      .filter {
        it.hasAnnotation(FeignClient::class.java.canonicalName)
      }

    val map = classes.filter { it.interfaces.isNotEmpty() }.associate { feignClientClassInfo ->
      feignClientClassInfo.interfaces.first().loadClass().kotlin to feignClientClassInfo.loadClass().kotlin
    }
    apiToFeignClientClassMapping.putAll(map)
  }
}

inline fun <reified T : Any> api(): T {
  val kClass = apiToFeignClientClassMapping[T::class]
  return kClass?.java?.let { DiServiceContext.ctx.getBean(it) as T }
    ?: throw IllegalArgumentException("no FeignClient found for ${T::class.java.simpleName}")
}
