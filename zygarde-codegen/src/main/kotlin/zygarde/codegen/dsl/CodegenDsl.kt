package zygarde.codegen.dsl

import zygarde.codegen.generator.Codegen
import zygarde.codegen.model.CodegenConfig

class CodegenDsl(private val config: CodegenConfig) {
  private val codegen by lazy { Codegen(config) }
  fun dto(vararg dtoArr: DtoDsl) {
    dtoArr.forEach { dto ->
      codegen.addDto("${config.basePackageName}.dto.${dto.name}", *dto.superClasses())
    }
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
