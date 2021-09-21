package zygarde.codegen.generator.impl

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Component
import zygarde.codegen.ZygardeKaptOptions.Companion.DAO_COMBINE
import zygarde.codegen.ZygardeKaptOptions.Companion.DAO_PACKAGE
import zygarde.codegen.ZygardeKaptOptions.Companion.DAO_SUFFIX
import zygarde.codegen.extension.kotlinpoet.ElementExtensions.fieldName
import zygarde.codegen.extension.kotlinpoet.ElementExtensions.name
import zygarde.codegen.extension.kotlinpoet.ElementExtensions.resolveGenericFieldTypeMap
import zygarde.codegen.extension.kotlinpoet.ElementExtensions.typeName
import zygarde.codegen.extension.kotlinpoet.generic
import zygarde.codegen.extension.kotlinpoet.kotlin
import zygarde.codegen.extension.kotlinpoet.kotlinTypeName
import zygarde.codegen.generator.AbstractZygardeGenerator
import zygarde.core.exception.CommonErrorCode
import zygarde.core.extension.exception.errWhenNull
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.persistence.Id
import javax.persistence.IdClass

class ZygardeJpaDaoGenerator(
  processingEnv: ProcessingEnvironment
) : AbstractZygardeGenerator(processingEnv) {

  fun generateDaoForEntityElements(elements: Collection<Element>) {
    if (elements.isEmpty()) {
      return
    }
    val daoPackage = packageName(processingEnv.options.getOrDefault(DAO_PACKAGE, "data.dao"))
    val daoSuffix = processingEnv.options.getOrDefault(DAO_SUFFIX, "Dao")
    elements.map { element ->
      "${element.name()}$daoSuffix".also { daoName ->
        FileSpec.builder(daoPackage, daoName)
          .addType(
            TypeSpec.interfaceBuilder(daoName)
              .addSuperinterface(
                JpaRepository::class.generic(element.typeName(), element.findIdClass())
              )
              .addSuperinterface(
                JpaSpecificationExecutor::class.generic(element.typeName())
              )
              .build()
          )
          .build()
          .writeTo(folderToGenerate())
      }
    }

    if (processingEnv.options.getOrDefault(DAO_COMBINE, "true") == "true") {
      val classBuilder = TypeSpec.classBuilder("Dao").addAnnotation(Component::class)
      val constructorBuilder = FunSpec.constructorBuilder()

      elements.forEach {
        val daoFieldName = "${it.fieldName()}$daoSuffix"
        val daoClass = ClassName(daoPackage, "${it.name()}$daoSuffix")
        classBuilder.addProperty(
          PropertySpec
            .builder(daoFieldName, daoClass)
            .initializer(daoFieldName)
            .addAnnotation(Autowired::class)
            .build()
        )
        constructorBuilder.addParameter(
          ParameterSpec
            .builder(daoFieldName, daoClass)
            .build()
        )
      }

      FileSpec.builder(daoPackage, "Dao")
        .addType(
          classBuilder
            .primaryConstructor(constructorBuilder.build())
            .build()
        )
        .build()
        .writeTo(folderToGenerate())
    }
  }

  private fun Element.findIdClass(): TypeName {
    val idClassAnnotation = this.getAnnotation(IdClass::class.java)
    if (idClassAnnotation != null) {
      return safeGetTypeFromAnnotation { idClassAnnotation.value.asTypeName() }.kotlin(canBeNullable = false)
    }
    val allFieldsIncludeSuper = this.allFieldsIncludeSuper()
    val idElement = allFieldsIncludeSuper
      .find { it.getAnnotation(Id::class.java) != null }
      .errWhenNull(CommonErrorCode.ERROR, "no id class found for entity ${this.simpleName}")
    val genericFieldTypeMap = this.resolveGenericFieldTypeMap(processingEnv)
    val idTypeMirror = idElement.asType()
    val idFieldLocation = "${idElement.enclosingElement}_$idTypeMirror"
    return genericFieldTypeMap.getOrElse(idFieldLocation) {
      idTypeMirror.kotlinTypeName(false)
    }
  }
}
