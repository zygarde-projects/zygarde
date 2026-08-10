package zygarde.codegen.dsl.generator

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import zygarde.codegen.dsl.meta.DtoMetaResolver
import zygarde.codegen.dsl.model.internal.DtoFieldMapping
import zygarde.codegen.dsl.model.internal.DtoSortableFieldPath
import zygarde.codegen.dsl.model.type.ValueProviderParameterType
import zygarde.codegen.generator.shared.addSchemaRequiredMode
import zygarde.codegen.meta.CodegenSealedInterface
import zygarde.core.annotation.Comment
import zygarde.data.api.OpenApiSortableFields
import zygarde.data.api.PagingAndSortingRequest
import java.io.Serializable
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

class DtoFieldMappingCodeGenerator(
  val dtoFieldMappings: Collection<DtoFieldMapping>,
  val sealedInterfaces: Collection<CodegenSealedInterface>,
  val dtoSortableFieldPaths: Collection<DtoSortableFieldPath>,
) {
  constructor(
    dtoFieldMappings: Collection<DtoFieldMapping>,
  ) : this(dtoFieldMappings, emptyList(), emptyList())

  constructor(
    dtoFieldMappings: Collection<DtoFieldMapping>,
    sealedInterfaces: Collection<CodegenSealedInterface>,
  ) : this(dtoFieldMappings, sealedInterfaces, emptyList())

  val dtoPackageName = System.getProperty("zygarde.codegen.dsl.model-mapping.dto-package", "zygarde.codegen.data.dto")
  val modelExtensionPackageName = System.getProperty("zygarde.codegen.dsl.model-mapping.extension-package", "zygarde.codegen.model.extensions")
  private val mergePatchFieldClass = ClassName("zygarde.json.patch", "MergePatchField")
  var dtoToExtraToDtoMappingMap = dtoFieldMappings
    .filter { it.modelField.extra && it is DtoFieldMapping.ModelToDtoFieldMappingVo }
    .filterNot { it.compound }
    .groupBy { it.modelField.modelClass.simpleName }
    .entries
    .associate {
      it.key to it.value.groupBy { it.dto }
    }
  val dtoToSealedInterfaces: Map<String, List<CodegenSealedInterface>> = sealedInterfaces
    .flatMap { sealed -> sealed.subtypes.map { it.dto.name to sealed } }
    .groupBy({ it.first }, { it.second })
  private val sortableFieldPathsByDto = dtoSortableFieldPaths
    .groupBy { it.dto }
    .mapValues { (_, paths) -> paths.distinctBy { it.path } }

  fun generateFileSpec(): DtoFieldMappingGenerateResult {
    validatePatchReqDtos()
    validateSortableFieldDtos()
    return DtoFieldMappingGenerateResult(
      dtoFileSpecs = listOf(
        generateDtos(),
        generateSealedInterfaces(),
        generateObjectSubtypes(),
      ).flatten(),
      modelMappingFileSpecs = listOf(
        generateDtoExtraValue(),
        generateMapToDtoExtension(),
        generateDtoAssemblers(),
        generateApplyFromDtoExtension(),
        generateApplyPatchExtension(),
        generateCompoundDtoBuilder(),
      ).flatten()
    )
  }

  private fun validateSortableFieldDtos() {
    sortableFieldPathsByDto.forEach { (dto, paths) ->
      val superClass = dto.superClass()
      require(superClass != null && PagingAndSortingRequest::class.java.isAssignableFrom(superClass.java)) {
        "DTO '${dto.name}' declares sortable fields but does not extend PagingAndSortingRequest."
      }
      val rootModelClasses = paths.map { it.rootModelClass }.distinct()
      require(rootModelClasses.size == 1) {
        "Sortable fields for DTO '${dto.name}' must share the same root model, but found: ${rootModelClasses.joinToString()}."
      }
    }
  }

  private fun validatePatchReqDtos() {
    dtoFieldMappings
      .groupBy { it.dto }
      .forEach { (dto, mappings) ->
        val containsPatchReqMapping = mappings.any { it is DtoFieldMapping.PatchReqFieldMapping }
        val containsNonPatchReqMapping = mappings.any { it !is DtoFieldMapping.PatchReqFieldMapping }
        if (containsPatchReqMapping && containsNonPatchReqMapping) {
          throw IllegalArgumentException("Patch request DTO '${dto.name}' cannot mix patchReq mappings with from/field/applyTo mappings.")
        }
      }
  }

  private fun generateCompoundDtoBuilder(): List<FileSpec> {
    return dtoFieldMappings
      .filter { it.compound }
      .mapNotNull { if (it is DtoFieldMapping.ModelToDtoFieldMappingVo) it else null }
      .groupBy { it.dto }
      .map { e ->
        val dto = e.key
        val mappings = e.value
        val dtoBuilderClassName = "${dto.name}Builder"
        val dtoBuilderFileSpecBuilder = FileSpec.builder(modelExtensionPackageName, dtoBuilderClassName)
        val dtoBuilderClassBuilder = TypeSpec.objectBuilder(ClassName(modelExtensionPackageName, dtoBuilderClassName))

        val dtoClass = ClassName(dtoPackageName, dto.name)
        val funcBuilder = FunSpec.builder("build")
          .returns(dtoClass)

        mappings
          .filterNot { it.modelField.extra }
          .map { it.modelField.modelClass }
          .toSet()
          .filter { it != Any::class.asClassName() }
          .forEach { modelClass ->
            funcBuilder
              .addParameter(
                modelClass.simpleName.replaceFirstChar { it.lowercase() },
                modelClass
              )
          }

        mappings
          .filter { it.modelField.extra }
          .filter { it.dataProvider == null }
          .forEach { mapping ->
            funcBuilder
              .addParameter(
                mapping.modelField.fieldName,
                mapping.fieldType(),
              )
          }

        // if (mappings.any { it.modelField.extra }) {
        //   funcBuilder
        //     .addParameter(
        //       ParameterSpec("extraValues", ClassName(dtoPackageName, "${dto.name}CompoundExtraValues"))
        //     )
        // }

        val callDtoArgs: MutableList<Any> = mutableListOf(dtoClass)
        val callDtoStatements = mappings
          .map { mapping ->
            val modelParamName = mapping.modelField.modelClass.simpleName.replaceFirstChar { it.lowercase() }
            val fieldName = mapping.modelField.fieldName
            val valueProviderParameterField = mapping.valueProviderParameterField
            val valueProvider = mapping.valueProvider
            if (mapping.dataProvider != null) {
              "$fieldName = ${mapping.providerPlaceholderExpression()}"
            } else if (valueProvider != null) {
              val valueProviderParam = when (mapping.valueProviderParameterType) {
                ValueProviderParameterType.FIELD -> "$modelParamName.$valueProviderParameterField"
                ValueProviderParameterType.OBJECT -> modelParamName
              }
              callDtoArgs.add(valueProvider)
              "$fieldName = %T().getValue($valueProviderParam)"
            } else if (mapping.modelField.extra) {
              "$fieldName = $fieldName"
            } else {
              "$fieldName = $modelParamName.$fieldName"
            }
          }
          .joinToString(",\r\n")

        funcBuilder.addCode(
          """return %T(
$callDtoStatements
)""",
          *callDtoArgs.toTypedArray()
        )

        dtoBuilderFileSpecBuilder
          .addType(
            dtoBuilderClassBuilder
              .addFunction(funcBuilder.build())
              .build()
          )
          .build()
      }
  }

  private fun generateDtos(): List<FileSpec> {
    val mappingsByDto = dtoFieldMappings.groupBy { it.dto }
    val dtos = linkedSetOf<zygarde.codegen.meta.CodegenDto>().also {
      it.addAll(mappingsByDto.keys)
      it.addAll(sortableFieldPathsByDto.keys)
    }
    return dtos.map { dto ->
      val mappings = mappingsByDto[dto].orEmpty()
      val dtoClassName = ClassName(dtoPackageName, dto.name)
      val dtoFileBuilder = FileSpec.builder(dtoClassName.packageName, dtoClassName.simpleName)
      val fieldNameToInterfacePropertyMap = dto.superClass()?.takeIf { it.java.isInterface }?.memberProperties?.associateBy { it.name } ?: emptyMap()
      val fieldNameToMappingMap = mappings.associateBy { it.modelField.fieldName }
      val dtoClassBuilder = TypeSpec.classBuilder(dtoClassName)
        .addAnnotation(Schema::class)
        .addSuperinterface(Serializable::class)
      if (fieldNameToInterfacePropertyMap.isNotEmpty() || fieldNameToMappingMap.isNotEmpty()) {
        dtoClassBuilder.addModifiers(KModifier.DATA)
      }
      sortableFieldPathsByDto[dto]?.let { paths ->
        val values = CodeBlock.builder().apply {
          paths.forEachIndexed { index, path ->
            if (index > 0) add(", ")
            add("%S", path.path)
          }
        }.build()
        dtoClassBuilder.addAnnotation(
          AnnotationSpec.builder(OpenApiSortableFields::class)
            .addMember("value = [%L]", values)
            .build()
        )
      }
      dto.superClass()?.also { superClass ->
        if (superClass.java.isInterface) {
          dtoClassBuilder.addSuperinterface(superClass)
        } else {
          dtoClassBuilder.superclass(superClass)
        }
      }
      dto.superInterfaces().forEach { i ->
        dtoClassBuilder.addSuperinterface(i)
      }
      dtoToSealedInterfaces[dto.name]?.forEach { sealed ->
        dtoClassBuilder.addSuperinterface(ClassName(dtoPackageName, sealed.name))
      }

      dto.annotations().forEach { a ->
        dtoClassBuilder.addAnnotation(a)
      }
      if (mappings.any { it is DtoFieldMapping.PatchReqFieldMapping }) {
        dtoClassBuilder.addAnnotation(
          AnnotationSpec.builder(ClassName("com.fasterxml.jackson.annotation", "JsonIgnoreProperties"))
            .addMember("ignoreUnknown = false")
            .build()
        )
        dtoClassBuilder.addFunction(
          FunSpec.builder("rejectUnknownPatchField")
            .addAnnotation(ClassName("com.fasterxml.jackson.annotation", "JsonAnySetter"))
            .addAnnotation(
              AnnotationSpec.builder(Suppress::class)
                .addMember("%S", "UNUSED_PARAMETER")
                .build()
            )
            .addParameter("fieldName", String::class)
            .addParameter("value", Any::class.asTypeName().copy(nullable = true))
            .addStatement("throw %T(%S + fieldName + %S)", IllegalArgumentException::class, "Unknown JSON merge patch field '", "'")
            .build()
        )
      }

      val dtoConstructorBuilder = FunSpec.constructorBuilder()

      listOf(fieldNameToInterfacePropertyMap.keys, fieldNameToMappingMap.keys).flatten().toSet().forEach { fieldName ->
        val memberFromSuperInterface = fieldNameToInterfacePropertyMap[fieldName]
        val mapping = fieldNameToMappingMap[fieldName]
        val fieldType = memberFromSuperInterface?.returnType?.asTypeName() ?: mapping?.fieldType() ?: return@forEach
        val comment = mapping?.comment ?: mapping?.modelField?.comment ?: memberFromSuperInterface?.findAnnotation<Comment>()?.comment
        dtoConstructorBuilder.addParameter(
          ParameterSpec
            .builder(fieldName, fieldType)
            .also {
              if (fieldType.isNullable) {
                it.defaultValue("null")
              } else if (mapping is DtoFieldMapping.PatchReqFieldMapping) {
                it.defaultValue("%T.Absent", mergePatchFieldClass)
              } else {
                if (fieldType is ParameterizedTypeName) {
                  if (fieldType.rawType == Collection::class.asClassName()) {
                    it.defaultValue("%M()", MemberName("kotlin.collections", "emptyList"))
                  } else if (fieldType.rawType == List::class.asClassName()) {
                    it.defaultValue("%M()", MemberName("kotlin.collections", "emptyList"))
                  } else if (fieldType.rawType == Set::class.asClassName()) {
                    it.defaultValue("%M()", MemberName("kotlin.collections", "emptySet"))
                  }
                }
              }
            }
            .build()
        )
        dtoClassBuilder.addProperty(
          PropertySpec
            .builder(fieldName, fieldType)
            .initializer(fieldName)
            .mutable(true)
            .also { p ->
              if (memberFromSuperInterface != null) {
                p.addModifiers(KModifier.OVERRIDE)
              }
            }
            .addAnnotation(
              buildSchemaAnnotation(mapping, fieldType, comment.orEmpty())
            )
            .also { p ->
              mapping?.additionalAnnotations?.forEach { a -> p.addAnnotation(a) }
              mapping?.validations?.toSet()?.forEach {
                p.addAnnotation(it.buildAnnotation())
              }
            }
            .build()
        )
      }

      dtoFileBuilder
        .addType(
          dtoClassBuilder
            .primaryConstructor(dtoConstructorBuilder.build())
            .build()
        )
        .build()
    }
  }

  private fun generateSealedInterfaces(): List<FileSpec> {
    return sealedInterfaces.map { sealed ->
      val sealedClassName = ClassName(dtoPackageName, sealed.name)
      val fileBuilder = FileSpec.builder(dtoPackageName, sealed.name)
      val interfaceBuilder = TypeSpec.interfaceBuilder(sealedClassName)
        .addModifiers(KModifier.SEALED)

      // @JsonTypeInfo
      interfaceBuilder.addAnnotation(
        AnnotationSpec.builder(ClassName("com.fasterxml.jackson.annotation", "JsonTypeInfo"))
          .addMember("use = %T.%L", ClassName("com.fasterxml.jackson.annotation", "JsonTypeInfo", "Id"), "NAME")
          .addMember("property = %S", sealed.discriminatorProperty)
          .build()
      )

      // @JsonSubTypes
      val jsonSubTypesBuilder = AnnotationSpec.builder(ClassName("com.fasterxml.jackson.annotation", "JsonSubTypes"))
      val subtypeCodeBlock = CodeBlock.builder().add("value = [")
      sealed.subtypes.forEachIndexed { idx, subtype ->
        if (idx > 0) subtypeCodeBlock.add(", ")
        subtypeCodeBlock.add(
          "%T(value = %T::class, name = %S)",
          ClassName("com.fasterxml.jackson.annotation", "JsonSubTypes", "Type"),
          ClassName(dtoPackageName, subtype.dto.name),
          subtype.discriminatorValue,
        )
      }
      subtypeCodeBlock.add("]")
      jsonSubTypesBuilder.addMember(subtypeCodeBlock.build())
      interfaceBuilder.addAnnotation(jsonSubTypesBuilder.build())

      // @Schema
      val schemaBuilder = AnnotationSpec.builder(Schema::class)
      val oneOfCodeBlock = CodeBlock.builder().add("oneOf = [")
      sealed.subtypes.forEachIndexed { idx, subtype ->
        if (idx > 0) oneOfCodeBlock.add(", ")
        oneOfCodeBlock.add("%T::class", ClassName(dtoPackageName, subtype.dto.name))
      }
      oneOfCodeBlock.add("]")
      schemaBuilder.addMember(oneOfCodeBlock.build())
      schemaBuilder.addMember("discriminatorProperty = %S", sealed.discriminatorProperty)
      interfaceBuilder.addAnnotation(schemaBuilder.build())

      fileBuilder.addType(interfaceBuilder.build()).build()
    }
  }

  private fun generateObjectSubtypes(): List<FileSpec> {
    val generatedDtoNames = (dtoFieldMappings.map { it.dto.name } + dtoSortableFieldPaths.map { it.dto.name }).toSet()
    return dtoToSealedInterfaces
      .filterKeys { it !in generatedDtoNames }
      .map { (dtoName, sealedList) ->
        val dtoClassName = ClassName(dtoPackageName, dtoName)
        val fileBuilder = FileSpec.builder(dtoPackageName, dtoName)
        val objectBuilder = TypeSpec.objectBuilder(dtoClassName)
          .addModifiers(KModifier.DATA)
          .addAnnotation(Schema::class)
          .addSuperinterface(Serializable::class)
        sealedList.forEach { sealed ->
          objectBuilder.addSuperinterface(ClassName(dtoPackageName, sealed.name))
        }
        fileBuilder.addType(objectBuilder.build()).build()
      }
  }

  private fun generateDtoExtraValue(): List<FileSpec> {
    return dtoToExtraToDtoMappingMap
      .flatMap { (modelClassName, dtoMappings) ->
        dtoMappings.map { e ->
          val dto = e.key
          val mappings = e.value
          val extraValuesName = "${modelClassName}To${dto.name}ExtraValues"
          val extraValueClass = ClassName(dtoPackageName, extraValuesName)
          val extraValueClassConstructorBuilder = FunSpec.constructorBuilder()
          val extraValueClassBuilder = TypeSpec.classBuilder(extraValueClass)
            .addModifiers(KModifier.DATA)
            .addSuperinterface(Serializable::class)

          mappings.forEach { mapping ->
            val fieldName = mapping.modelField.fieldName
            extraValueClassConstructorBuilder.addParameter(
              ParameterSpec
                .builder(fieldName, mapping.fieldType())
                .build()
            )
            extraValueClassBuilder.addProperty(
              PropertySpec
                .builder(fieldName, mapping.fieldType())
                .initializer(fieldName)
                .build()
            )
          }

          extraValueClassBuilder
            .primaryConstructor(extraValueClassConstructorBuilder.build())

          FileSpec.builder(dtoPackageName, extraValuesName)
            .addType(extraValueClassBuilder.build())
            .build()
        }
      }
  }

  private fun generateMapToDtoExtension(): List<FileSpec> {
    return dtoFieldMappings
      .filterNot { it.compound }
      .mapNotNull { if (it is DtoFieldMapping.ModelToDtoFieldMappingVo) it else null }
      .groupBy { it.modelField.modelClass }
      .map { e ->
        val modelClass = e.key
        val modelClassName = "${modelClass.simpleName}"
        val extensionClassName = "${modelClassName}ToDtoExtensions"
        val extensionFileSpecBuilder = FileSpec.builder(modelExtensionPackageName, extensionClassName)
        val extensionClassBuilder = TypeSpec.objectBuilder(ClassName(modelExtensionPackageName, extensionClassName))

        e.value.groupBy { it.dto }.forEach { (dto, mappingsByDto) ->
          val codeBlockArgs = mutableListOf<Any>(ClassName(dtoPackageName, dto.name))
          val toDtoFuncParameters = mutableListOf<ParameterSpec>()

          if (mappingsByDto.any { it.modelField.extra }) {
            toDtoFuncParameters.add(
              ParameterSpec("extraValues", ClassName(dtoPackageName, "${modelClassName}To${dto.name}ExtraValues"))
            )
          }

          val dtoFieldSetterStatements = mappingsByDto.map { mapping ->
            val dtoFieldName = mapping.modelField.fieldName
            val modelFieldName = mapping.modelField.fieldName
            val dtoRef = mapping.dtoRef
            val q = if (mapping.modelField.fieldNullable) "?" else ""
            val valueProvider = mapping.valueProvider
            val valueProviderParameterType = mapping.valueProviderParameterType
            val valueProviderParameterField = mapping.valueProviderParameterField
            val isExtraField = mapping.modelField.extra
            if (mapping.dataProvider != null) {
              "  $dtoFieldName = ${mapping.providerPlaceholderExpression()}"
            } else if (valueProvider != null) {
              codeBlockArgs.add(valueProvider)
              val valueProviderParam = when (valueProviderParameterType) {
                ValueProviderParameterType.FIELD -> "this.$valueProviderParameterField"
                ValueProviderParameterType.OBJECT -> "this"
              }

              "  $dtoFieldName = %T().getValue($valueProviderParam)"
            } else if (isExtraField) {
              "  $dtoFieldName = extraValues.$modelFieldName"
            } else if (dtoRef != null) {
              val modelForToDtoExtensions = mapping.modelField.fieldClass.toString()
              codeBlockArgs.add(
                MemberName(
                  "$modelExtensionPackageName.${modelForToDtoExtensions}ToDtoExtensions",
                  "to${dtoRef.name}"
                )
              )
              if (mapping.refCollection) {
                "  $dtoFieldName = this.$modelFieldName$q.map{it.%M()}"
              } else {
                if (dtoToExtraToDtoMappingMap[modelClassName]?.get(dtoRef) != null) {
                  toDtoFuncParameters.add(
                    ParameterSpec(
                      "${modelFieldName}ExtraValues",
                      ClassName(dtoPackageName, "${modelClassName}To${dtoRef.name}ExtraValues")
                    )
                  )
                  "  $dtoFieldName = this.$modelFieldName$q.%M(${modelFieldName}ExtraValues)"
                } else {
                  "  $dtoFieldName = this.$modelFieldName$q.%M()"
                }
              }
            } else {
              "  $dtoFieldName = this.$modelFieldName"
            }
          }

          extensionClassBuilder.addFunction(
            FunSpec.builder("to${dto.name}")
              .receiver(modelClass)
              .returns(ClassName(dtoPackageName, dto.name))
              .also { fb ->
                toDtoFuncParameters.forEach {
                  fb.addParameter(it)
                }
              }
              .addStatement(
                """return %T(
${dtoFieldSetterStatements.joinToString(",\r\n")}              
)
                """.trimMargin(),
                *codeBlockArgs.toTypedArray()
              )
              .build()
          )
        }

        extensionFileSpecBuilder
          .addType(extensionClassBuilder.build())
          .build()
      }
  }

  private fun generateDtoAssemblers(): List<FileSpec> {
    return dtoFieldMappings
      .mapNotNull { if (it is DtoFieldMapping.ModelToDtoFieldMappingVo) it else null }
      .groupBy { it.dto }
      .mapNotNull { (dto, mappingsByDto) ->
        val modelClasses = mappingsByDto
          .map { it.modelField.modelClass }
          .filter { it != Any::class.asClassName() }
          .toSet()
        val hasUnsupportedExtraField = mappingsByDto.any { it.modelField.extra && it.dataProvider == null }
        if (modelClasses.size != 1 || hasUnsupportedExtraField) {
          null
        } else {
          val modelClass = modelClasses.single()
          val dtoClass = ClassName(dtoPackageName, dto.name)
          val assemblerClassName = "${dto.name}Assembler"
          val assemblerClass = ClassName(modelExtensionPackageName, assemblerClassName)
          val providerMappings = mappingsByDto.filter { it.dataProvider != null }
          providerMappings.forEach { mapping ->
            requireNotNull(mapping.dataProviderKeyField) {
              "Data provider field '${dto.name}.${mapping.modelField.fieldName}' requires key(...)."
            }
          }

          val constructor = FunSpec.constructorBuilder()
          val assemblerBuilder = TypeSpec.classBuilder(assemblerClass)
            .addAnnotation(Component::class)
            .addFunction(
              FunSpec.builder("build")
                .addParameter("model", modelClass)
                .returns(dtoClass)
                .addStatement("return buildAll(listOf(model)).single()")
                .build()
            )

          providerMappings
            .map { requireNotNull(it.dataProvider) }
            .distinct()
            .forEach { provider ->
              val providerPropertyName = provider.providerPropertyName()
              constructor.addParameter(
                ParameterSpec.builder(providerPropertyName, provider)
                  .addAnnotation(Autowired::class)
                  .build()
              )
              assemblerBuilder.addProperty(
                PropertySpec.builder(providerPropertyName, provider, KModifier.PRIVATE)
                  .initializer(providerPropertyName)
                  .build()
              )
            }

          val buildAllBuilder = FunSpec.builder("buildAll")
            .addParameter("models", Collection::class.asClassName().parameterizedBy(modelClass))
            .returns(Collection::class.asClassName().parameterizedBy(dtoClass))
            .addStatement("val modelList = models.toList()")

          providerMappings.forEach { mapping ->
            val keyField = requireNotNull(mapping.dataProviderKeyField)
            val provider = requireNotNull(mapping.dataProvider)
            buildAllBuilder.addStatement(
              "val %N = modelList.mapNotNull·{ it.%N }.distinct()",
              mapping.providerKeysVariableName(),
              keyField.fieldName,
            )
            buildAllBuilder.addStatement(
              "val %N = %N.load(%N)",
              mapping.providerValuesVariableName(),
              provider.providerPropertyName(),
              mapping.providerKeysVariableName(),
            )
          }

          val codeBlockArgs = mutableListOf<Any>(dtoClass)
          val dtoFieldSetterStatements = mappingsByDto.map { mapping ->
            val dtoFieldName = mapping.modelField.fieldName
            val modelFieldName = mapping.modelField.fieldName
            val dtoRef = mapping.dtoRef
            val q = if (mapping.modelField.fieldNullable) "?" else ""
            val valueProvider = mapping.valueProvider
            val valueProviderParameterType = mapping.valueProviderParameterType
            val valueProviderParameterField = mapping.valueProviderParameterField
            val isExtraField = mapping.modelField.extra
            if (mapping.dataProvider != null) {
              "  $dtoFieldName = ${mapping.providerValueExpression("model")}"
            } else if (valueProvider != null) {
              codeBlockArgs.add(valueProvider)
              val valueProviderParam = when (valueProviderParameterType) {
                ValueProviderParameterType.FIELD -> "model.$valueProviderParameterField"
                ValueProviderParameterType.OBJECT -> "model"
              }

              "  $dtoFieldName = %T().getValue($valueProviderParam)"
            } else if (isExtraField) {
              "  $dtoFieldName = ${mapping.providerPlaceholderExpression()}"
            } else if (dtoRef != null) {
              val modelForToDtoExtensions = mapping.modelField.fieldClass.toString()
              codeBlockArgs.add(
                MemberName(
                  "$modelExtensionPackageName.${modelForToDtoExtensions}ToDtoExtensions",
                  "to${dtoRef.name}"
                )
              )
              if (mapping.refCollection) {
                "  $dtoFieldName = model.$modelFieldName$q.map{it.%M()}"
              } else {
                "  $dtoFieldName = model.$modelFieldName$q.%M()"
              }
            } else {
              "  $dtoFieldName = model.$modelFieldName"
            }
          }

          buildAllBuilder.addStatement(
            """return modelList.map·{ model ->
%T(
${dtoFieldSetterStatements.joinToString(",\r\n")}
)
}""",
            *codeBlockArgs.toTypedArray(),
          )

          FileSpec.builder(modelExtensionPackageName, assemblerClassName)
            .addType(
              assemblerBuilder
                .primaryConstructor(constructor.build())
                .addFunction(buildAllBuilder.build())
                .build()
            )
            .build()
        }
      }
  }

  private fun generateApplyFromDtoExtension(): List<FileSpec> {
    return dtoFieldMappings
      .mapNotNull { if (it is DtoFieldMapping.ModelApplyFromDtoFieldMappingVo) it else null }
      .groupBy { it.modelField.modelClass }
      .map { e ->
        val modelClass = e.key
        val extensionClassName = "${modelClass.simpleName}ApplyValueExtensions"
        val extensionFileSpecBuilder = FileSpec.builder(modelExtensionPackageName, extensionClassName)
        val extensionClassBuilder = TypeSpec.objectBuilder(ClassName(modelExtensionPackageName, extensionClassName))

        e.value.groupBy { it.dto }.forEach { dto, mappings ->
          val functionBuilder = FunSpec.builder("applyFrom")
            .addParameter("req", ClassName(dtoPackageName, dto.name))
          // if (modelClass.isAbstract) { // TODO check logic
          //   functionBuilder
          //     .addTypeVariable(TypeVariableName("T", modelClass))
          //     .receiver(TypeVariableName("T"))
          //     .returns(TypeVariableName("T"))
          // } else {
          functionBuilder
            .receiver(modelClass)
            .returns(modelClass)
          // }

          mappings.forEach { mapping ->
            val modelFieldName = mapping.modelField.fieldName
            val dtoFieldName = mapping.modelField.fieldName
            val valueProvider = mapping.valueProvider
            if (valueProvider != null) {
              functionBuilder.addStatement(
                "this.$modelFieldName = %T().getValue(req.$dtoFieldName)",
                valueProvider
              )
            } else {
              functionBuilder.addStatement("this.$modelFieldName = req.$dtoFieldName")
            }
          }

          extensionClassBuilder.addFunction(
            functionBuilder.addStatement("return this").build()
          )
        }

        extensionFileSpecBuilder
          .addType(extensionClassBuilder.build())
          .build()
      }
  }

  private fun generateApplyPatchExtension(): List<FileSpec> {
    return dtoFieldMappings
      .mapNotNull { if (it is DtoFieldMapping.PatchReqFieldMapping) it else null }
      .groupBy { it.modelField.modelClass }
      .map { e ->
        val modelClass = e.key
        val extensionClassName = "${modelClass.simpleName}PatchExtensions"
        val extensionFileSpecBuilder = FileSpec.builder(modelExtensionPackageName, extensionClassName)
        val extensionClassBuilder = TypeSpec.objectBuilder(ClassName(modelExtensionPackageName, extensionClassName))

        e.value.groupBy { it.dto }.forEach { dto, mappings ->
          val functionBuilder = FunSpec.builder("applyPatch")
            .addParameter("req", ClassName(dtoPackageName, dto.name))
            .receiver(modelClass)
            .returns(modelClass)

          mappings.forEach { mapping ->
            val fieldName = mapping.modelField.fieldName
            functionBuilder.beginControlFlow("when (val patchField = req.$fieldName)")
            functionBuilder.addStatement("%T.Absent -> Unit", mergePatchFieldClass)
            if (mapping.modelField.fieldNullable) {
              functionBuilder.addStatement("%T.NullValue -> this.$fieldName = null", mergePatchFieldClass)
            } else {
              functionBuilder.addStatement(
                "%T.NullValue -> require(false)·{ %S }",
                mergePatchFieldClass,
                "JSON merge patch field '$fieldName' cannot be null",
              )
            }
            functionBuilder.addStatement("is %T.Value -> this.$fieldName = patchField.value", mergePatchFieldClass)
            functionBuilder.endControlFlow()
          }

          extensionClassBuilder.addFunction(
            functionBuilder.addStatement("return this").build()
          )
        }

        extensionFileSpecBuilder
          .addType(extensionClassBuilder.build())
          .build()
      }
  }

  private fun DtoFieldMapping.fieldType(): TypeName {
    val resolvedFieldType = DtoMetaResolver.resolveFieldType(this, dtoPackageName)
    return if (this is DtoFieldMapping.PatchReqFieldMapping) {
      mergePatchFieldClass.parameterizedBy(resolvedFieldType.copy(nullable = false))
    } else {
      resolvedFieldType
    }
  }

  private fun ClassName.providerPropertyName(): String {
    return simpleName.replaceFirstChar { it.lowercase() }
  }

  private fun DtoFieldMapping.ModelToDtoFieldMappingVo.providerKeysVariableName(): String {
    return "${modelField.fieldName}Keys"
  }

  private fun DtoFieldMapping.ModelToDtoFieldMappingVo.providerValuesVariableName(): String {
    return "${modelField.fieldName}Values"
  }

  private fun DtoFieldMapping.ModelToDtoFieldMappingVo.providerPlaceholderExpression(): String {
    return if (fieldType().isNullable) {
      "null"
    } else {
      """error("Data provider field '${dto.name}.${modelField.fieldName}' requires ${dto.name}Assembler")"""
    }
  }

  private fun DtoFieldMapping.ModelToDtoFieldMappingVo.providerValueExpression(modelRef: String): String {
    val provider = requireNotNull(dataProvider)
    val keyField = requireNotNull(dataProviderKeyField)
    val keyExpression = "$modelRef.${keyField.fieldName}"
    val valuesVariable = providerValuesVariableName()
    return if (fieldType().isNullable) {
      "$keyExpression?.let { $valuesVariable[it] }"
    } else {
      val missingNullKeyMessage = "Data provider ${provider.simpleName} did not return value for field '${modelField.fieldName}' and key 'null'"
      "$keyExpression?.let { key -> $valuesVariable[key] ?: throw IllegalStateException(" +
        "\"Data provider ${provider.simpleName} did not return value for field '${modelField.fieldName}' and key '\$key'\") } " +
        "?: throw IllegalStateException(\"$missingNullKeyMessage\")"
    }
  }

  private fun TypeName.schemaImplementationType(): TypeName {
    return when (this) {
      is ParameterizedTypeName -> rawType
      else -> copy(nullable = false)
    }
  }

  private fun buildSchemaAnnotation(
    mapping: DtoFieldMapping?,
    fieldType: TypeName,
    comment: String,
  ): AnnotationSpec {
    if (mapping is DtoFieldMapping.PatchReqFieldMapping) {
      val modelFieldType = mapping.modelField.fieldClass
      if (modelFieldType.isOpenApiArrayType()) {
        return AnnotationSpec.builder(ArraySchema::class)
          .addMember(
            "arraySchema = %T(description = %S, nullable = %L, requiredMode = %T.RequiredMode.NOT_REQUIRED)",
            Schema::class,
            comment,
            mapping.modelField.fieldNullable,
            Schema::class,
          )
          .addMember("schema = %T(implementation = %T::class)", Schema::class, modelFieldType.openApiArrayItemImplementationType())
          .build()
      }

      return AnnotationSpec.builder(Schema::class)
        .addMember("description=%S", comment)
        .addMember("implementation = %T::class", modelFieldType.schemaImplementationType())
        .addMember("nullable = %L", mapping.modelField.fieldNullable)
        .addSchemaRequiredMode(false)
        .build()
    }

    return AnnotationSpec.builder(Schema::class)
      .addMember("description=%S", comment)
      .addSchemaRequiredMode(!fieldType.isNullable)
      .build()
  }

  private fun TypeName.isOpenApiArrayType(): Boolean {
    return this is ParameterizedTypeName &&
      rawType in setOf(Collection::class.asClassName(), List::class.asClassName(), Set::class.asClassName())
  }

  private fun TypeName.openApiArrayItemImplementationType(): TypeName {
    return if (this is ParameterizedTypeName) {
      typeArguments.firstOrNull()?.schemaImplementationType() ?: Any::class.asTypeName()
    } else {
      Any::class.asTypeName()
    }
  }
}
