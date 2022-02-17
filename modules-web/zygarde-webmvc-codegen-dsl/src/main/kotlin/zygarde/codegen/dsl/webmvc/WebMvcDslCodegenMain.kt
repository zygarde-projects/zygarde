package zygarde.codegen.dsl.webmvc

import com.squareup.kotlinpoet.FileSpec
import io.github.classgraph.ClassGraph
import org.springframework.util.FileSystemUtils
import zygarde.codegen.generator.WebMvcApiGenerator
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
      it.extendsSuperclass(WebMvcDslCodegen::class.java.canonicalName)
    }
    .filter {
      !it.isAbstract
    }

  val codegenInstanceList = classes.loadClasses().map { clz ->
    clz.newInstance() as WebMvcDslCodegen
  }

  codegenInstanceList.forEach { it.codegen() }

  val apisToGenerate = codegenInstanceList.flatMap { it.apisToGenerate }
  val generateResults = WebMvcApiGenerator(apisToGenerate).generateApis()

  generateResults.apiInterfaces.writeSpecToFileOrSysOut("zygarde.codegen.dsl.webmvc.api-interface.write-to")
  generateResults.feignApiInterfaces.writeSpecToFileOrSysOut("zygarde.codegen.dsl.webmvc.feign-interface.write-to")
  generateResults.controllers.writeSpecToFileOrSysOut("zygarde.codegen.dsl.webmvc.controller.write-to")
  generateResults.serviceInterfaces.writeSpecToFileOrSysOut("zygarde.codegen.dsl.webmvc.service-interface.write-to")
}
