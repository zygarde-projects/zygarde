package zygarde.codegen.dsl

import zygarde.codegen.generator.Codegen

class EntityFieldDsl(
  val entityClass: String,
  val codegen: Codegen,
  val fieldName: String,
  val fieldType: String,
  var comment: String = ""
) {
  fun toDto(vararg dtoArr: CodegenDto) {
    for (dto in dtoArr) {
      codegen.addMappingEntityFieldToDto(entityClass, fieldName, fieldType, dto.name)
    }
  }
}
