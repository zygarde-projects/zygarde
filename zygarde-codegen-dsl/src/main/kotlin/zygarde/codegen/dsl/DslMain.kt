package zygarde.codegen.dsl

import io.github.classgraph.ClassGraph
import zygarde.codegen.dsl.generator.ModelToDtoMappingGenerator

fun main() {
  val classes = ClassGraph()
    .enableClassInfo()
    .enableAnnotationInfo()
    .scan()
    .allClasses
    .filter {
      it.extendsSuperclass(DslModelMappingCodegen::class.java.canonicalName)
    }
    .filter {
      !it.isAbstract
    }

  val modelMappingCodegenList = classes.loadClasses().map { clz ->
    clz.newInstance() as DslModelMappingCodegen<*>
  }

  modelMappingCodegenList.forEach { it.execte() }

  val modelFieldToDtoMappings = modelMappingCodegenList.flatMap { it.modelFieldToDtoMappings }

  ModelToDtoMappingGenerator(modelFieldToDtoMappings)
    .generateFileSpec()
    .forEach {
      it.writeTo(System.out)
    }
}
