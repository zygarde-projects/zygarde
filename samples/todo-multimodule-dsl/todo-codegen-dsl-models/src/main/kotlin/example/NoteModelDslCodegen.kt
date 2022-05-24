package example

import example.codegen.model.meta.AbstractAbstractNoteCodegen
import zygarde.codegen.meta.CodegenDtoSimple

class NoteModelDslCodegen : AbstractAbstractNoteCodegen() {

  enum class NoteDtos : CodegenDtoSimple {
    UpdateNoteReq,
  }

  override fun codegen() {
    req(NoteDtos.UpdateNoteReq) {
      applyTo(title)
    }
  }
}
