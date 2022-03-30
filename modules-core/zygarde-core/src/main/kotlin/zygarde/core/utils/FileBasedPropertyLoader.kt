package zygarde.core.utils

import zygarde.core.log.Loggable
import java.io.File

object FileBasedPropertyLoader : Loggable {

  fun loadFromDirectory(directoryPath: String) {
    val resolvedProps = File(directoryPath)
      .takeIf { it.exists() && it.isDirectory }
      ?.listFiles()
      ?.mapNotNull { propertyFile ->
        propertyFile.readLines()
          .firstOrNull()
          ?.replace(System.lineSeparator(), "")
          ?.let { value ->
            propertyFile.name to value
          }
      }
      ?.toMap()
      ?: emptyMap()

    if (resolvedProps.isNotEmpty()) {
      LOGGER.trace("props loaded from $directoryPath:\r\n${resolvedProps.map { (k, v) -> "$k=$v" }.joinToString("\r\n")}")
      resolvedProps.forEach { (k, v) ->
        System.setProperty(k, v)
      }
    }
  }
}
