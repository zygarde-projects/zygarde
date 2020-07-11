package zygarde.codegen.value

import zygarde.data.jpa.entity.AutoLongIdEntity
import zygarde.data.jpa.entity.getId

class AutoLongIdValueProvider : ValueProvider<AutoLongIdEntity, Long> {
  override fun getValue(v: AutoLongIdEntity): Long = v.getId()
}
