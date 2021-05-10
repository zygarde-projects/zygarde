package zygarde.codegen.dsl

import zygarde.codegen.generator.Codegen

class EntityFieldDsl(
  val entityClass: String,
  val codegen: Codegen,
  var comment: String = ""
) {
  fun toDto(vararg dtoArr: CodegenDto) {
  }
}
