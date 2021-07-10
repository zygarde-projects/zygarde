package zygarde.codegen.dsl

import io.github.classgraph.ClassGraph
import zygarde.codegen.dsl.generator.ModelToDtoMappingGenerator

fun main() {
  val classes = ClassGraph()
    .enableClassInfo()
    .enableAnnotationInfo()
    .scan()
    .allClasses
    .filter { it.extendsSuperclass(DslModelMappingCodegen::class.java.canonicalName) }

  val modelMappingCodegenList = classes.loadClasses().map { clz ->
    clz.newInstance() as DslModelMappingCodegen
  }

  modelMappingCodegenList.forEach { it.execte() }

  ModelToDtoMappingGenerator()
    .generateFileSpec(
      modelMappingCodegenList.flatMap { it.modelFieldToDtoMappings }
    )
    .forEach {
      it.writeTo(System.out)
    }
}
