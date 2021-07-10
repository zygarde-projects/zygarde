package zygarde.codegen.generator.impl

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import zygarde.codegen.ZygardeKaptOptions
import zygarde.codegen.extension.kotlinpoet.kotlin
import zygarde.codegen.generator.AbstractZygardeGenerator
import zygarde.codegen.meta.CodegenDto
import zygarde.codegen.meta.RegisterDtos
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import kotlin.reflect.KClass

class ZygardeRegisterDtosGenerator(
  processingEnv: ProcessingEnvironment
) : AbstractZygardeGenerator(processingEnv) {

  fun generateDtos(elements: Collection<Element>) {
    val superClassEnumPropertyType = KClass::class.asTypeName().parameterizedBy(STAR).kotlin(true)
    val superClassRefEnumPropertyType = String::class.asTypeName().kotlin(true)
    elements.map { it.getAnnotation(RegisterDtos::class.java) }
      .forEach { registerDtos ->
        if (registerDtos.values.isNotEmpty()) {
          val className = "${registerDtos.group}Dtos"

          val enumBuilder = TypeSpec.enumBuilder(className)
            .addSuperinterface(CodegenDto::class)
            .primaryConstructor(
              FunSpec.constructorBuilder()
                .addParameter("superClass", superClassEnumPropertyType)
                .addParameter("superClassRef", superClassRefEnumPropertyType)
                .build()
            )

          registerDtos.values.forEach { registerDto ->
            val superClassTypeName = safeGetTypeFromAnnotation { registerDto.superClass.asTypeName() }
            registerDto.dtos.forEach { dto ->
              val anonymousClassBuilder = TypeSpec.anonymousClassBuilder()
              if (superClassTypeName.toString() != "java.lang.Object") {
                anonymousClassBuilder.addSuperclassConstructorParameter("%T::class", superClassTypeName)
              } else {
                anonymousClassBuilder.addSuperclassConstructorParameter("null")
              }
              if (registerDto.superClassRef.isNotEmpty()) {
                anonymousClassBuilder.addSuperclassConstructorParameter("%s", registerDto.superClassRef)
              } else {
                anonymousClassBuilder.addSuperclassConstructorParameter("null")
              }
              enumBuilder.addEnumConstant(dto, anonymousClassBuilder.build())
            }
          }

          enumBuilder
            .addProperty(
              PropertySpec.builder("superClass", superClassEnumPropertyType)
                .initializer("superClass")
                .build()
            )
            .addProperty(
              PropertySpec.builder("superClassRef", superClassRefEnumPropertyType)
                .initializer("superClassRef")
                .build()
            )

          enumBuilder
            .addFunction(
              FunSpec.builder("superClass")
                .addModifiers(KModifier.OVERRIDE)
                .returns(superClassEnumPropertyType)
                .addCode("return superClass")
                .build()
            )
            .addFunction(
              FunSpec.builder("superClassRef")
                .addModifiers(KModifier.OVERRIDE)
                .returns(superClassRefEnumPropertyType)
                .addCode("return superClassRef")
                .build()
            )

          val pack = packageName(processingEnv.options.getOrDefault(ZygardeKaptOptions.BASE_PACKAGE, "dto"))
          FileSpec.builder(pack, className)
            .addType(enumBuilder.build())
            .build()
            .writeTo(folderToGenerate())
        }
      }
  }
}
