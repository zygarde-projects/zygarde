package zygarde.codegen.dsl.graphql

import com.squareup.kotlinpoet.FileSpec
import io.github.classgraph.ClassGraph
import org.springframework.util.FileSystemUtils
import zygarde.codegen.generator.GraphQlApiGenerator
import zygarde.codegen.model.graphql.GraphQlSchemaGenerateResult
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

private fun List<GraphQlSchemaGenerateResult>.writeSchemaToFileOrSysOut(propertyName: String) {
  val target = System.getProperty(propertyName)
    ?.let {
      File(it).also { f ->
        FileSystemUtils.deleteRecursively(f)
        f.mkdirs()
      }
    }
  forEach { schema ->
    if (target != null) {
      File(target, schema.fileName).writeText(schema.content)
    } else {
      print(schema.content)
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
      it.extendsSuperclass(GraphQlDslCodegen::class.java.canonicalName)
    }
    .filter {
      !it.isAbstract
    }

  val codegenInstanceList = classes.loadClasses().map { clz ->
    clz.getDeclaredConstructor().newInstance() as GraphQlDslCodegen
  }

  codegenInstanceList.forEach { it.codegen() }

  val apisToGenerate = codegenInstanceList.flatMap { it.apisToGenerate }
  val generateResults = GraphQlApiGenerator(apisToGenerate).generateApis()

  generateResults.controllers.writeSpecToFileOrSysOut("zygarde.codegen.dsl.graphql.controller.write-to")
  generateResults.serviceInterfaces.writeSpecToFileOrSysOut("zygarde.codegen.dsl.graphql.service-interface.write-to")
  generateResults.schemas.writeSchemaToFileOrSysOut("zygarde.codegen.dsl.graphql.schema.write-to")
}
