package zygarde.codegen.generator

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SubscriptionMapping
import org.springframework.stereotype.Controller
import zygarde.codegen.model.graphql.GraphQlApiToGenerateVo
import zygarde.codegen.model.graphql.GraphQlArgumentToGenerateVo
import zygarde.codegen.model.graphql.GraphQlFieldToGenerateVo
import zygarde.codegen.model.graphql.GraphQlGenerateResult
import zygarde.codegen.model.graphql.GraphQlFunctionToGenerateVo
import zygarde.codegen.model.graphql.GraphQlOperation
import zygarde.codegen.model.graphql.GraphQlSchemaGenerateResult
import zygarde.codegen.model.graphql.GraphQlTypeDefinitionKind
import zygarde.codegen.model.graphql.GraphQlTypeDefinitionToGenerateVo
import zygarde.codegen.model.graphql.requireGraphQlName

class GraphQlApiGenerator(
  private val apis: Collection<GraphQlApiToGenerateVo>
) {
  private val controllerFileSpecBuilderMap = mutableMapOf<String, FileSpec.Builder>()
  private val controllerBuilderMap = mutableMapOf<String, TypeSpec.Builder>()
  private val serviceInterfaceFileSpecBuilderMap = mutableMapOf<String, FileSpec.Builder>()
  private val serviceInterfaceBuilderMap = mutableMapOf<String, TypeSpec.Builder>()

  private val beanFunc = MemberName("zygarde.core.di.DiServiceContext", "bean")

  fun generateApis(): GraphQlGenerateResult {
    apis.forEach { it.validateGraphQlNames() }
    validateUniqueGraphQlDeclarations()
    apis.forEach { it.generate() }
    val emittedOperationTypes = mutableSetOf<String>()

    controllerFileSpecBuilderMap.forEach { (controllerName, fileSpecBuilder) ->
      controllerBuilderMap[controllerName]?.build()?.let(fileSpecBuilder::addType)
    }
    serviceInterfaceFileSpecBuilderMap.forEach { (serviceName, fileSpecBuilder) ->
      serviceInterfaceBuilderMap[serviceName]?.build()?.let(fileSpecBuilder::addType)
    }

    return GraphQlGenerateResult(
      controllers = controllerFileSpecBuilderMap.values.map { it.build() },
      serviceInterfaces = serviceInterfaceFileSpecBuilderMap.values.map { it.build() },
      schemas = apis.map { it.toSchemaGenerateResult(emittedOperationTypes) },
    )
  }

  private fun GraphQlApiToGenerateVo.validateGraphQlNames() {
    functions.forEach { function ->
      requireGraphQlName(function.functionName, "GraphQL ${function.operation.name.lowercase()} field")
      requireGraphQlName(function.responseGraphQlType, "GraphQL response type")
      function.arguments.forEach { argument ->
        requireGraphQlName(argument.name, "GraphQL argument name")
        requireGraphQlName(argument.graphQlType, "GraphQL argument type")
      }
    }
    typeDefinitions.forEach { it.validateGraphQlNames() }
  }

  private fun GraphQlTypeDefinitionToGenerateVo.validateGraphQlNames() {
    requireGraphQlName(name, "GraphQL type definition name")
    when (kind) {
      GraphQlTypeDefinitionKind.TYPE,
      GraphQlTypeDefinitionKind.INPUT -> {
        require(fields.isNotEmpty()) {
          "GraphQL ${kind.schemaKeyword()} '$name' must declare at least one field"
        }
        fields.forEach { field ->
          requireGraphQlName(field.name, "GraphQL field name")
          requireGraphQlName(field.graphQlType, "GraphQL field type")
        }
      }
      GraphQlTypeDefinitionKind.ENUM -> {
        require(enumValues.isNotEmpty()) {
          "GraphQL enum '$name' must declare at least one value"
        }
        enumValues.forEach { enumValue ->
          requireGraphQlName(enumValue, "GraphQL enum value")
        }
      }
      GraphQlTypeDefinitionKind.SCALAR -> Unit
    }
  }

  private fun validateUniqueGraphQlDeclarations() {
    GraphQlOperation.entries.forEach { operation ->
      apis.flatMap { api -> api.functions.filter { it.operation == operation } }
        .map { it.functionName }
        .firstDuplicateOrNull()
        ?.let { duplicateName ->
          throw IllegalArgumentException("GraphQL ${operation.name.lowercase()} field '$duplicateName' is already declared")
        }
    }

    apis.flatMap { it.typeDefinitions }
      .map { it.name }
      .firstDuplicateOrNull()
      ?.let { duplicateName ->
        throw IllegalArgumentException("GraphQL type definition '$duplicateName' is already declared")
      }

    apis.flatMap { it.functions }.forEach { function ->
      function.arguments.map { it.name }
        .firstDuplicateOrNull()
        ?.let { duplicateName ->
          throw IllegalArgumentException(
            "GraphQL ${function.operation.name.lowercase()} field '${function.functionName}' argument '$duplicateName' is already declared"
          )
        }
    }

    apis.flatMap { it.typeDefinitions }.forEach { typeDefinition ->
      when (typeDefinition.kind) {
        GraphQlTypeDefinitionKind.TYPE,
        GraphQlTypeDefinitionKind.INPUT -> {
          typeDefinition.fields.map { it.name }
            .firstDuplicateOrNull()
            ?.let { duplicateName ->
              throw IllegalArgumentException(
                "GraphQL ${typeDefinition.kind.schemaKeyword()} '${typeDefinition.name}' field '$duplicateName' is already declared"
              )
            }
        }
        GraphQlTypeDefinitionKind.ENUM -> {
          typeDefinition.enumValues.firstDuplicateOrNull()
            ?.let { duplicateName ->
              throw IllegalArgumentException("GraphQL enum '${typeDefinition.name}' value '$duplicateName' is already declared")
            }
        }
        GraphQlTypeDefinitionKind.SCALAR -> Unit
      }
    }
  }

  private fun GraphQlApiToGenerateVo.generate() {
    if (functions.isEmpty()) {
      return
    }

    val controllerName = "${apiName}Controller"
    controllerFileSpecBuilderMap.getOrPut(controllerName) {
      FileSpec.builder(controllerPackage, controllerName)
    }
    val controllerBuilder = controllerBuilderMap.getOrPut(controllerName) {
      TypeSpec.classBuilder(controllerName)
        .addAnnotation(Controller::class)
    }

    functions.forEach { function ->
      val serviceInterfaceName = function.serviceName ?: "${apiName}Service"
      val serviceFunctionName = function.serviceFunctionName ?: function.functionName
      serviceInterfaceFileSpecBuilderMap.getOrPut(serviceInterfaceName) {
        FileSpec.builder(serviceInterfacePackage, serviceInterfaceName)
      }
      val serviceInterfaceBuilder = serviceInterfaceBuilderMap.getOrPut(serviceInterfaceName) {
        TypeSpec.interfaceBuilder(serviceInterfaceName)
      }

      val serviceFuncBuilder = FunSpec.builder(serviceFunctionName)
        .addModifiers(KModifier.ABSTRACT)
        .returns(function.kotlinResponseType())

      val controllerFuncBuilder = FunSpec.builder(function.functionName)
        .addAnnotation(function.toMappingAnnotationSpec())
        .returns(function.kotlinResponseType())

      val paramsToCallServiceInterface = mutableListOf<String>()
      function.arguments.forEach { argument ->
        val parameter = argument.toParameterSpec()
        serviceFuncBuilder.addParameter(parameter)
        controllerFuncBuilder.addParameter(
          parameter.toBuilder()
            .addAnnotation(argument.toArgumentAnnotationSpec())
            .build()
        )
        paramsToCallServiceInterface.add(argument.name)
      }

      controllerFuncBuilder.addStatement(
        "val service = %M<%T>()",
        beanFunc,
        ClassName(serviceInterfacePackage, serviceInterfaceName)
      )
      controllerFuncBuilder.addStatement(
        "return service.$serviceFunctionName(${paramsToCallServiceInterface.joinToString(", ")})"
      )

      controllerBuilder.addFunction(controllerFuncBuilder.build())
      serviceFuncBuilder.build()
        .takeUnless(serviceInterfaceBuilder.funSpecs::contains)
        ?.let(serviceInterfaceBuilder::addFunction)
    }
  }

  private fun GraphQlArgumentToGenerateVo.toParameterSpec(): ParameterSpec {
    val parameterType = if (collection) {
      type.toCollectionType(itemNullable = itemNullable, nullable = nullable)
    } else {
      type.copy(nullable = nullable)
    }
    return ParameterSpec.builder(name, parameterType).build()
  }

  private fun GraphQlFunctionToGenerateVo.toMappingAnnotationSpec(): AnnotationSpec {
    val annotation = when (operation) {
      GraphQlOperation.QUERY -> QueryMapping::class
      GraphQlOperation.MUTATION -> MutationMapping::class
      GraphQlOperation.SUBSCRIPTION -> SubscriptionMapping::class
    }
    return AnnotationSpec.builder(annotation)
      .addMember("name = %S", functionName)
      .build()
  }

  private fun GraphQlArgumentToGenerateVo.toArgumentAnnotationSpec(): AnnotationSpec {
    return AnnotationSpec.builder(Argument::class)
      .addMember("name = %S", name)
      .build()
  }

  private fun zygarde.codegen.model.graphql.GraphQlFunctionToGenerateVo.kotlinResponseType(): TypeName {
    return if (responseCollection) {
      responseType.toCollectionType(itemNullable = responseItemNullable, nullable = responseNullable)
    } else {
      responseType.copy(nullable = responseNullable)
    }
  }

  private fun TypeName.toCollectionType(itemNullable: Boolean, nullable: Boolean): TypeName {
    return Collection::class.asTypeName()
      .parameterizedBy(copy(nullable = itemNullable))
      .copy(nullable = nullable)
  }

  private fun GraphQlApiToGenerateVo.toSchemaGenerateResult(
    emittedOperationTypes: MutableSet<String>
  ): GraphQlSchemaGenerateResult {
    val schema = buildString {
      appendOperationType(functions.filter { it.operation == GraphQlOperation.QUERY }, "Query", emittedOperationTypes)
      appendOperationType(functions.filter { it.operation == GraphQlOperation.MUTATION }, "Mutation", emittedOperationTypes)
      appendOperationType(functions.filter { it.operation == GraphQlOperation.SUBSCRIPTION }, "Subscription", emittedOperationTypes)
      typeDefinitions.forEach { typeDefinition ->
        if (isNotEmpty()) {
          appendLine()
        }
        if (typeDefinition.kind == GraphQlTypeDefinitionKind.SCALAR) {
          appendLine("scalar ${typeDefinition.name}")
          return@forEach
        }
        appendLine("${typeDefinition.kind.schemaKeyword()} ${typeDefinition.name} {")
        when (typeDefinition.kind) {
          GraphQlTypeDefinitionKind.TYPE,
          GraphQlTypeDefinitionKind.INPUT -> {
            typeDefinition.fields.forEach { field ->
              val defaultValue = field.defaultValue.takeIf { typeDefinition.kind == GraphQlTypeDefinitionKind.INPUT }
              appendLine("  ${field.name}: ${field.toSchemaType()}${defaultValue.toSchemaDefaultValue()}")
            }
          }
          GraphQlTypeDefinitionKind.ENUM -> {
            typeDefinition.enumValues.forEach { enumValue ->
              appendLine("  $enumValue")
            }
          }
          GraphQlTypeDefinitionKind.SCALAR -> Unit
        }
        appendLine("}")
      }
    }.trimEnd() + "\n"

    return GraphQlSchemaGenerateResult(
      fileName = "${apiName.replaceFirstChar { it.lowercase() }}.graphqls",
      content = schema,
    )
  }

  private fun StringBuilder.appendOperationType(
    functions: List<GraphQlFunctionToGenerateVo>,
    typeName: String,
    emittedOperationTypes: MutableSet<String>
  ) {
    if (functions.isEmpty()) {
      return
    }

    if (isNotEmpty()) {
      appendLine()
    }
    val keyword = if (emittedOperationTypes.add(typeName)) {
      "type"
    } else {
      "extend type"
    }
    appendLine("$keyword $typeName {")
    functions.forEach { function ->
      appendLine("  ${function.functionName}${function.arguments.toSchemaArguments()}: ${function.toSchemaResponseType()}")
    }
    appendLine("}")
  }

  private fun List<GraphQlArgumentToGenerateVo>.toSchemaArguments(): String {
    if (isEmpty()) {
      return ""
    }
    return joinToString(prefix = "(", postfix = ")") { argument ->
      "${argument.name}: ${argument.toSchemaType()}${argument.defaultValue.toSchemaDefaultValue()}"
    }
  }

  private fun GraphQlArgumentToGenerateVo.toSchemaType(): String {
    val itemType = if (collection) {
      "[${graphQlType}${if (itemNullable) "" else "!"}]"
    } else {
      graphQlType
    }
    return itemType + if (nullable) "" else "!"
  }

  private fun GraphQlFunctionToGenerateVo.toSchemaResponseType(): String {
    return if (responseCollection) {
      "[${responseGraphQlType}${if (responseItemNullable) "" else "!"}]" + if (responseNullable) "" else "!"
    } else {
      responseGraphQlType + if (responseNullable) "" else "!"
    }
  }

  private fun GraphQlFieldToGenerateVo.toSchemaType(): String {
    val itemType = if (collection) {
      "[${graphQlType}${if (itemNullable) "" else "!"}]"
    } else {
      graphQlType
    }
    return itemType + if (nullable) "" else "!"
  }

  private fun String?.toSchemaDefaultValue(): String {
    return this?.let { " = $it" }.orEmpty()
  }

  private fun GraphQlTypeDefinitionKind.schemaKeyword(): String {
    return when (this) {
      GraphQlTypeDefinitionKind.TYPE -> "type"
      GraphQlTypeDefinitionKind.INPUT -> "input"
      GraphQlTypeDefinitionKind.ENUM -> "enum"
      GraphQlTypeDefinitionKind.SCALAR -> "scalar"
    }
  }

  private fun Iterable<String>.firstDuplicateOrNull(): String? {
    val seen = mutableSetOf<String>()
    return firstOrNull { !seen.add(it) }
  }
}
