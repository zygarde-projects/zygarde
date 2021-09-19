package zygarde.core.log

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author leo
 */
interface Loggable {
  val LOGGER: Logger
    get() = LoggerFactory.getLogger(javaClass)
}
