package zygarde.codegen.model.extensions

import example.Mark
import zygarde.codegen.`data`.dto.SaveMarkReq

public object MarkApplyValueExtensions {
  public fun Mark.applyFrom(req: SaveMarkReq): Mark {
    this.x = req.x
    this.y = req.y
    return this
  }
}
