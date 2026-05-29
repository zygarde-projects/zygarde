package zygarde.codegen.dsl.graphql

import com.squareup.kotlinpoet.FileSpec
import io.github.classgraph.ClassGraph
import io.github.classgraph.ScanResult
import org.springframework.util.FileSystemUtils
import zygarde.codegen.dsl.ModelMappingDslCodegen
import zygarde.codegen.dsl.meta.DtoMetaResolver
import zygarde.codegen.dsl.meta.ModelMappingMetadata
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

private fun scanConcreteSubclasses(scan: ScanResult, superClass: Class<*>) =
  scan.allClasses
    .filter { it.extendsSuperclass(superClass.canonicalName) }
    .filter { !it.isAbstract }
    .loadClasses()

fun main() {
  val scan = ClassGraph()
    .enableClassInfo()
    .enableAnnotationInfo()
    .scan()

  val modelMappingMetadata: ModelMappingMetadata = scanConcreteSubclasses(scan, ModelMappingDslCodegen::class.java)
    .map { clz ->
      (clz.getDeclaredConstructor().newInstance() as ModelMappingDslCodegen).also { it.execute() }
    }
    .flatMap { it.dtoFieldMappings }
    .let { DtoMetaResolver.resolve(it) }

  val codegenInstanceList = scanConcreteSubclasses(scan, GraphQlDslCodegen::class.java)
    .map { clz -> clz.getDeclaredConstructor().newInstance() as GraphQlDslCodegen }

  codegenInstanceList.forEach {
    it.modelMappingMetadata = modelMappingMetadata
    it.codegen()
  }

  val apisToGenerate = codegenInstanceList.flatMap { it.apisToGenerate }
  val generateResults = GraphQlApiGenerator(apisToGenerate).generateApis()

  generateResults.controllers.writeSpecToFileOrSysOut("zygarde.codegen.dsl.graphql.controller.write-to")
  (generateResults.serviceInterfaces + generateResults.supportTypes)
    .writeSpecToFileOrSysOut("zygarde.codegen.dsl.graphql.service-interface.write-to")
  generateResults.schemas.writeSchemaToFileOrSysOut("zygarde.codegen.dsl.graphql.schema.write-to")
}
