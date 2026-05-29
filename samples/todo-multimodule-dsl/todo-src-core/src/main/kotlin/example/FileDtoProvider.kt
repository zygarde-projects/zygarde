package example

import org.springframework.stereotype.Component
import zygarde.data.provider.DataProvider
import zygarde.data.provider.DataProviderContext

data class FileDto(
  val id: String,
  val name: String,
)

@Component
class FileDtoProvider : DataProvider<String, FileDto> {
  override fun load(keys: Collection<String>, context: DataProviderContext): Map<String, FileDto> {
    return keys.associateWith { key -> FileDto(id = key, name = "File $key") }
  }
}
