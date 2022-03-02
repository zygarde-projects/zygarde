package zygarde.codegen.dsl

import com.squareup.kotlinpoet.FileSpec
import io.github.classgraph.ClassGraph
import org.springframework.util.FileSystemUtils
import zygarde.codegen.dsl.generator.DtoFieldMappingCodeGenerator
import java.io.File

private fun List<FileSpec>.writeSpecToFileOrSysOut(propertyName: String) {
  val target = System.getProperty(propertyName)
    ?.let {
      File(it).also { f ->
        FileSystemUtils.deleteRecursively(f)
        f.mkdirs()
      }
    }
  forEach { fileSpec ->
    if (target != null) {
      fileSpec.writeTo(target)
    } else {
      fileSpec.writeTo(System.out)
    }
  }
}

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

  val result = DtoFieldMappingCodeGenerator(dtoFieldMappings)
    .generateFileSpec()

  result.dtoFileSpecs.writeSpecToFileOrSysOut("zygarde.codegen.dsl.dto.write-to")
  result.modelMappingFileSpecs.writeSpecToFileOrSysOut("zygarde.codegen.dsl.model-mapping.write-to")
}
