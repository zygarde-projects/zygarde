package zygarde.codegen.value

import zygarde.data.jpa.entity.AutoIntIdEntity
import zygarde.data.jpa.entity.getId

class AutoIntIdValueProvider : ValueProvider<AutoIntIdEntity, Int> {
  override fun getValue(v: AutoIntIdEntity): Int = v.getId()
}
