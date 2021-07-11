package zygarde.codegen.dsl

import io.github.classgraph.ClassGraph
import org.springframework.util.FileSystemUtils
import zygarde.codegen.dsl.generator.ModelToDtoMappingGenerator
import java.io.File

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

  val codegenTarget = System.getProperty("zygarde.codegen.target")
    ?.let {
      File(it).also { f ->
        FileSystemUtils.deleteRecursively(f)
        f.mkdirs()
      }
    }

  ModelToDtoMappingGenerator(modelFieldToDtoMappings)
    .generateFileSpec()
    .forEach {
      if (codegenTarget != null) {
        it.writeTo(codegenTarget)
      } else {
        it.writeTo(System.out)
      }
    }
}