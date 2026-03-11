package zygarde.codegen.dsl

import zygarde.codegen.meta.CodegenDto
import zygarde.codegen.meta.CodegenSealedInterface
import zygarde.codegen.meta.SealedSubtypeMapping

class SealedInterfaceSpec(val name: String) {
  var discriminatorProperty: String = "type"
  private val _subtypes = mutableListOf<SealedSubtypeMapping>()

  fun subtype(discriminatorValue: String, dto: CodegenDto) {
    _subtypes.add(SealedSubtypeMapping(discriminatorValue, dto))
  }

  fun build() = CodegenSealedInterface(name, discriminatorProperty, _subtypes.toList())
}
