package zygarde.fixture

import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationListener
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import zygarde.core.log.Loggable

@Order(Ordered.LOWEST_PRECEDENCE)
class FixtureRunner(
  private val applicationContext: ApplicationContext
) : Loggable, ApplicationListener<ApplicationReadyEvent> {

  var fixtureRan = false

  override fun onApplicationEvent(event: ApplicationReadyEvent) {
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
