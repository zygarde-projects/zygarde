package zygarde.codegen.dsl

import zygarde.codegen.generator.Codegen
import zygarde.codegen.model.CodegenConfig

class CodegenDsl(val config: CodegenConfig) {
  val codegen by lazy { Codegen(config) }

  inline fun <reified E : CodegenDto> dto() {
    E::class.java.enumConstants.forEach { dto ->
      codegen.getOrAddDtoBuilders(config.dtoNameToClass(dto.name), *dto.superClasses().toTypedArray())
    }
  }

  fun dtoFields(vararg dtoArr: CodegenDto, dsl: DtoFieldDsl.() -> Unit) {
    dsl.invoke(
      DtoFieldDsl(
        codegen,
        dtoArr.toList().map { config.dtoNameToClass(it.name) }
      )
    )
  }

  fun generate() {
    codegen.allFileSpecs().forEach {
      it.writeTo(System.out)
    }
  }
}

fun codegen(config: CodegenConfig = CodegenConfig(), dsl: CodegenDsl.() -> Unit) {
  val codegen = CodegenDsl(config)
  dsl.invoke(codegen)
  codegen.generate()
}
