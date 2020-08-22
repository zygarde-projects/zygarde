package zygarde.codegen.value

import zygarde.data.jpa.entity.AutoIdGetter
import zygarde.data.jpa.entity.getId

class AutoLongIdValueProvider : ValueProvider<AutoIdGetter<Long>, Long> {
  override fun getValue(v: AutoIdGetter<Long>): Long = v.getId()
}
