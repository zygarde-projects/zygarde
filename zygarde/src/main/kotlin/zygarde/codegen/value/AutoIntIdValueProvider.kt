package zygarde.codegen.value

import zygarde.data.jpa.entity.AutoIdGetter
import zygarde.data.jpa.entity.getId

class AutoIntIdValueProvider : ValueProvider<AutoIdGetter<Int>, Int> {
  override fun getValue(v: AutoIdGetter<Int>): Int = v.getId()
}
