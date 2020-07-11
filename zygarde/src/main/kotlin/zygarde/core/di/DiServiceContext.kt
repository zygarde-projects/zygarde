package zygarde.core.di

import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

object DiServiceContext : ApplicationContextAware {
  lateinit var ctx: ApplicationContext
    private set
  val beanMap: MutableMap<Class<*>, Any> = mutableMapOf()
  override fun setApplicationContext(applicationContext: ApplicationContext) {
    ctx = applicationContext
  }

  inline fun <reified T : Any> bean(): T {
    return beanMap.getOrPut(T::class.java, { ctx.getBean<T>() }) as T
  }
}
