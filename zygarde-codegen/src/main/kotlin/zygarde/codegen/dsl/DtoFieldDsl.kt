package zygarde.codegen.dsl

import zygarde.codegen.generator.Codegen

class DtoFieldDsl(val codegen: Codegen, val dtoClasses: List<String>) {
  inline fun <reified F> extraField(fieldName: String, comment: String? = null, nullable: Boolean = false) {
    dtoClasses.forEach { dtoClass ->
      codegen.addExtraFieldToDto(dtoClass, fieldName, F::class.java.canonicalName, comment, nullable)
    }
  }
}
