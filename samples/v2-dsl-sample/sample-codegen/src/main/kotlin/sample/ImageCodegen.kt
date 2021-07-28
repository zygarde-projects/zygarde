package sample

import sample.ImageCodegen.ImageModels.ImageDto
import zygarde.codegen.meta.CodegenDtoSimple
import zygarde.generated.model.meta.AbstractImageCodegen

class ImageCodegen : AbstractImageCodegen() {

  enum class ImageModels : CodegenDtoSimple {
    ImageDto,
  }

  override fun codegen() {
    id { toDto(ImageDto) }
    url { toDto(ImageDto) }
  }
}
