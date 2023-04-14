package zygarde.codegen.model.extensions

import example.Note
import zygarde.codegen.`data`.dto.UpdateNoteReq

public object NoteApplyValueExtensions {
  public fun Note.applyFrom(req: UpdateNoteReq): Note {
    this.title = req.title
    return this
  }
}
