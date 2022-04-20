package zygarde.codegen.model.extensions

import example.AbstractNote
import zygarde.codegen.`data`.dto.UpdateNoteReq

public object AbstractNoteApplyValueExtensions {
  public fun <T : AbstractNote> T.applyFrom(req: UpdateNoteReq): T {
    this.title = req.title
    return this
  }
}
