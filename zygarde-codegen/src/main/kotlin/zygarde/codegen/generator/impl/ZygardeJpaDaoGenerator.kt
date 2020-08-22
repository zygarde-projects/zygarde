package zygarde.codegen.generator.impl

import com.squareup.kotlinpoet.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Component
import zygarde.codegen.ZygardeKaptOptions.Companion.DAO_COMBINE
import zygarde.codegen.ZygardeKaptOptions.Companion.DAO_PACKAGE
import zygarde.core.exception.CommonErrorCode
import zygarde.codegen.ZygardeKaptOptions.Companion.DAO_SUFFIX
import zygarde.codegen.extension.kotlinpoet.*
import zygarde.codegen.generator.AbstractZygardeGenerator
import zygarde.core.extension.exception.errWhenNull
import zygarde.data.jpa.entity.AutoIntIdEntity
import zygarde.data.jpa.entity.AutoLongIdEntity
import zygarde.data.jpa.entity.SequenceIntIdEntity
import zygarde.data.jpa.entity.SequenceLongIdEntity
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
          .writeTo(fileTarget)
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
        .writeTo(fileTarget)
    }
  }

  private fun Element.findIdClass(): TypeName {
    if (this.getAnnotation(IdClass::class.java) != null) {
      return safeGetTypeFromAnnotation { this.getAnnotation(IdClass::class.java).value.asTypeName() }.kotlin(canBeNullable = false)
    }
    val allSuperTypes = this.allSuperTypes(processingEnv)
    if (allSuperTypes.any { it.typeName() == AutoLongIdEntity::class.asTypeName() }) {
      return Long::class.asTypeName()
    } else if (allSuperTypes.any { it.typeName() == AutoIntIdEntity::class.asTypeName() }) {
      return Int::class.asTypeName()
    } else if (allSuperTypes.any { it.typeName() == SequenceLongIdEntity::class.asTypeName() }) {
      return Long::class.asTypeName()
    } else if (allSuperTypes.any { it.typeName() == SequenceIntIdEntity::class.asTypeName() }) {
      return Int::class.asTypeName()
    }

    val allFieldsIncludeSuper = this.allFieldsIncludeSuper()
    return allFieldsIncludeSuper
      .find { it.getAnnotation(Id::class.java) != null }
      .errWhenNull(CommonErrorCode.ERROR, "no id class found for entity ${this.simpleName}")
      .asType()
      .kotlinTypeName(false)
  }
}
