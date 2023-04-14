package example

import zygarde.codegen.dsl.ModelMappingCodegenSpec
import zygarde.codegen.meta.CodegenDtoSimple

class NoteModelDslCodegen : ModelMappingCodegenSpec({
  NoteDtos.UpdateNoteReq {
    applyTo(Note::title)
  }
}) {

  enum class NoteDtos : CodegenDtoSimple {
    UpdateNoteReq,
  }
}
