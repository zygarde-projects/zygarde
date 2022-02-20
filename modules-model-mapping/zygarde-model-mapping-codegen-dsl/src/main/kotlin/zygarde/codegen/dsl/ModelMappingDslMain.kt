package zygarde.codegen.dsl

import io.github.classgraph.ClassGraph
import org.springframework.util.FileSystemUtils
import zygarde.codegen.dsl.generator.DtoFieldMappingCodeGenerator
import java.io.File

fun main() {
  val classes = ClassGraph()
    .enableClassInfo()
    .enableAnnotationInfo()
    .scan()
    .allClasses
    .filter {
      it.extendsSuperclass(ModelMappingDslCodegen::class.java.canonicalName)
    }
    .filter {
      !it.isAbstract
    }

  val modelMappingCodegenList = classes.loadClasses().map { clz ->
    clz.newInstance() as ModelMappingDslCodegen<*>
  }

  modelMappingCodegenList.forEach { it.execute() }

  val dtoFieldMappings = modelMappingCodegenList.flatMap { it.dtoFieldMappings }

  val codegenTarget = System.getProperty("zygarde.codegen.dsl.model-mapping.write-to")
    ?.let {
      File(it).also { f ->
        FileSystemUtils.deleteRecursively(f)
        f.mkdirs()
      }
    }

  DtoFieldMappingCodeGenerator(dtoFieldMappings)
    .generateFileSpec()
    .forEach {
      if (codegenTarget != null) {
        it.writeTo(codegenTarget)
      } else {
        it.writeTo(System.out)
      }
    }
}
