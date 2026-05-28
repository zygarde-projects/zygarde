package zygarde.codegen.dsl.sqlapi

import com.squareup.kotlinpoet.FileSpec
import io.github.classgraph.ClassGraph
import org.springframework.util.FileSystemUtils
import java.io.File

private fun targetDir(propertyName: String): File? {
  return System.getProperty(propertyName)?.let(::File)
}

private fun Collection<File?>.prepareTargetDirs() {
  filterNotNull()
    .map { it.canonicalFile }
    .distinctBy { it.absolutePath }
    .forEach { file ->
      FileSystemUtils.deleteRecursively(file)
      file.mkdirs()
    }
}

private fun List<FileSpec>.writeSpecToFileOrSysOut(target: File?) {
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
      it.extendsSuperclass(SqlApiDslCodegen::class.java.canonicalName)
    }
    .filter {
      !it.isAbstract
    }

  val codegenInstanceList = classes.loadClasses().map { clazz ->
    clazz.getDeclaredConstructor().newInstance() as SqlApiDslCodegen
  }

  codegenInstanceList.forEach { it.codegen() }

  val apisToGenerate = codegenInstanceList.flatMap { it.apisToGenerate }
  val generateResults = SqlApiGenerator(apisToGenerate).generate()

  val dtoTarget = targetDir("zygarde.codegen.dsl.sql-api.dto.write-to")
  val apiInterfaceTarget = targetDir("zygarde.codegen.dsl.sql-api.api-interface.write-to")
  val feignInterfaceTarget = targetDir("zygarde.codegen.dsl.sql-api.feign-interface.write-to")
  val controllerTarget = targetDir("zygarde.codegen.dsl.sql-api.controller.write-to")
  val serviceInterfaceTarget = targetDir("zygarde.codegen.dsl.sql-api.service-interface.write-to")
  val serviceImplTarget = targetDir("zygarde.codegen.dsl.sql-api.service-impl.write-to")

  listOf(
    dtoTarget,
    apiInterfaceTarget,
    feignInterfaceTarget,
    controllerTarget,
    serviceInterfaceTarget,
    serviceImplTarget,
  ).prepareTargetDirs()

  generateResults.dtoFileSpecs.writeSpecToFileOrSysOut(dtoTarget)
  generateResults.webApiGenerateResult.apiInterfaces.writeSpecToFileOrSysOut(apiInterfaceTarget)
  generateResults.webApiGenerateResult.feignApiInterfaces.writeSpecToFileOrSysOut(feignInterfaceTarget)
  generateResults.webApiGenerateResult.controllers.writeSpecToFileOrSysOut(controllerTarget)
  generateResults.webApiGenerateResult.serviceInterfaces.writeSpecToFileOrSysOut(serviceInterfaceTarget)
  generateResults.serviceImplFileSpecs.writeSpecToFileOrSysOut(serviceImplTarget)
}
