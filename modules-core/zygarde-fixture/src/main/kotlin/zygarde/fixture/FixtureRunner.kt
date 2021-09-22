package zygarde.fixture

import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import zygarde.core.log.Loggable

@Order(Ordered.LOWEST_PRECEDENCE)
class FixtureRunner(
  private val applicationContext: ApplicationContext
) : Loggable, ApplicationListener<ContextRefreshedEvent> {

  var fixtureRan = false

  override fun onApplicationEvent(event: ContextRefreshedEvent) {
    runAllFixtures()
  }

  private fun runAllFixtures() {
    if (fixtureRan) {
      return
    }
    fixtureRan = true
    applicationContext.getBeansOfType(Fixture::class.java)
      .map { it.value }
      .also {
        LOGGER.info("${it.size} fixtures detected")
      }
      .sortedBy { it.order() }
      .forEach {
        LOGGER.info("running fixture ${it.javaClass.canonicalName}")
        it.run()
        LOGGER.info("fixture ${it.javaClass.canonicalName} completed")
      }
  }
}
