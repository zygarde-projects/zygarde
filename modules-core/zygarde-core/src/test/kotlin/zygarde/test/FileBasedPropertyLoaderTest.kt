package zygarde.test

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import zygarde.core.utils.FileBasedPropertyLoader
import java.io.File
import java.nio.file.Files

class FileBasedPropertyLoaderTest {

  @Test
  fun testLoadProperty() {
    val dir = Files.createTempDirectory("secret").toFile()
    File(dir, "my.test.props").writeText("foo")
    FileBasedPropertyLoader.loadFromDirectory(dir.absolutePath)

    System.getProperty("my.test.props") shouldBe "foo"
  }
}
