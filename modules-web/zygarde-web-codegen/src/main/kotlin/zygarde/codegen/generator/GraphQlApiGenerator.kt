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
import zygarde.codegen.model.graphql.graphQlStringLiteral
import zygarde.codegen.model.graphql.requireGraphQlDeprecationReason
import zygarde.codegen.model.graphql.requireGraphQlDescription
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
      requireGraphQlDescription(
        function.description,
        "GraphQL ${function.operation.name.lowercase()} field '${function.functionName}'"
      )
      function.arguments.forEach { argument ->
        requireGraphQlName(argument.name, "GraphQL argument name")
        requireGraphQlName(argument.graphQlType, "GraphQL argument type")
        requireGraphQlDescription(
          argument.description,
          "GraphQL ${function.operation.name.lowercase()} field '${function.functionName}' argument '${argument.name}'"
        )
      }
    }
    typeDefinitions.forEach { it.validateGraphQlNames() }
  }

  private fun GraphQlTypeDefinitionToGenerateVo.validateGraphQlNames() {
    requireGraphQlName(name, "GraphQL type definition name")
    requireGraphQlDescription(description, "GraphQL ${kind.schemaKeyword()} '$name'")
    when (kind) {
      GraphQlTypeDefinitionKind.TYPE,
      GraphQlTypeDefinitionKind.INPUT -> {
        require(fields.isNotEmpty()) {
          "GraphQL ${kind.schemaKeyword()} '$name' must declare at least one field"
        }
        fields.forEach { field ->
          require(field.defaultValue == null || kind == GraphQlTypeDefinitionKind.INPUT) {
            "GraphQL field default values are only supported on input fields"
          }
          requireGraphQlName(field.name, "GraphQL field name")
          requireGraphQlName(field.graphQlType, "GraphQL field type")
          requireGraphQlDescription(field.description, "GraphQL ${kind.schemaKeyword()} '$name' field '${field.name}'")
          requireGraphQlDeprecationReason(field.deprecationReason, "GraphQL ${kind.schemaKeyword()} '$name' field '${field.name}'")
          require(
            field.deprecationReason == null ||
              kind != GraphQlTypeDefinitionKind.INPUT ||
              field.nullable ||
              field.defaultValue != null
          ) {
            "GraphQL input '$name' field '${field.name}' cannot be deprecated because it is a required input field"
          }
        }
      }
      GraphQlTypeDefinitionKind.ENUM -> {
        require(enumValues.isNotEmpty()) {
          "GraphQL enum '$name' must declare at least one value"
        }
        enumValues.forEach { enumValue ->
          requireGraphQlName(enumValue.name, "GraphQL enum value")
          requireGraphQlDescription(enumValue.description, "GraphQL enum '$name' value '${enumValue.name}'")
        }
      }
      GraphQlTypeDefinitionKind.SCALAR -> Unit
    }
  }

  private fun validateUniqueGraphQlDeclarations() {
    apis.map { it.apiName }
      .firstDuplicateOrNull()
      ?.let { duplicateName ->
        throw IllegalArgumentException("GraphQL API '$duplicateName' is already declared")
      }

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
          typeDefinition.enumValues.map { it.name }
            .firstDuplicateOrNull()
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

    val duplicatedControllerFunctionNames = functions.groupingBy { it.functionName }
      .eachCount()
      .filterValues { it > 1 }
      .keys
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
      val controllerFunctionName = function.toControllerFunctionName(duplicatedControllerFunctionNames)
      val serviceFunctionName = function.serviceFunctionName ?: controllerFunctionName
      serviceInterfaceFileSpecBuilderMap.getOrPut(serviceInterfaceName) {
        FileSpec.builder(serviceInterfacePackage, serviceInterfaceName)
      }
      val serviceInterfaceBuilder = serviceInterfaceBuilderMap.getOrPut(serviceInterfaceName) {
        TypeSpec.interfaceBuilder(serviceInterfaceName)
      }

      val serviceFuncBuilder = FunSpec.builder(serviceFunctionName)
        .addModifiers(KModifier.ABSTRACT)
        .returns(function.kotlinResponseType())

      val controllerFuncBuilder = FunSpec.builder(controllerFunctionName)
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
        paramsToCallServiceInterface.add(argument.name.toKotlinReferenceName())
      }

      controllerFuncBuilder.addStatement(
        "val service = %M<%T>()",
        beanFunc,
        ClassName(serviceInterfacePackage, serviceInterfaceName)
      )
      controllerFuncBuilder.addStatement(
        "return service.${serviceFunctionName.toKotlinReferenceName()}(${paramsToCallServiceInterface.joinToString(", ")})"
      )

      controllerBuilder.addFunction(controllerFuncBuilder.build())
      serviceFuncBuilder.build()
        .takeUnless(serviceInterfaceBuilder.funSpecs::contains)
        ?.let(serviceInterfaceBuilder::addFunction)
    }
  }

  private fun GraphQlFunctionToGenerateVo.toControllerFunctionName(duplicatedControllerFunctionNames: Set<String>): String {
    if (functionName !in duplicatedControllerFunctionNames) {
      return functionName
    }
    return "${operation.name.lowercase()}${functionName.replaceFirstChar { it.uppercase() }}"
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
        append(typeDefinition.description.toSchemaDescription(""))
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
              append(field.description.toSchemaDescription("  "))
              appendLine(
                "  ${field.name}: ${field.toSchemaType()}" +
                  "${defaultValue.toSchemaDefaultValue()}${field.deprecationReason.toSchemaDeprecation()}"
              )
            }
          }
          GraphQlTypeDefinitionKind.ENUM -> {
            typeDefinition.enumValues.forEach { enumValue ->
              append(enumValue.description.toSchemaDescription("  "))
              appendLine("  ${enumValue.name}")
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
      append(function.description.toSchemaDescription("  "))
      appendLine("  ${function.functionName}${function.arguments.toSchemaArguments("  ")}: ${function.toSchemaResponseType()}")
    }
    appendLine("}")
  }

  private fun List<GraphQlArgumentToGenerateVo>.toSchemaArguments(fieldIndent: String): String {
    if (isEmpty()) {
      return ""
    }
    if (none { it.description != null }) {
      return joinToString(prefix = "(", postfix = ")") { argument ->
        "${argument.name}: ${argument.toSchemaType()}${argument.defaultValue.toSchemaDefaultValue()}"
      }
    }
    val argumentIndent = "$fieldIndent  "
    return buildString {
      appendLine("(")
      this@toSchemaArguments.forEach { argument ->
        append(argument.description.toSchemaDescription(argumentIndent))
        appendLine("$argumentIndent${argument.name}: ${argument.toSchemaType()}${argument.defaultValue.toSchemaDefaultValue()}")
      }
      append("$fieldIndent)")
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

  private fun String?.toSchemaDeprecation(): String {
    return this?.let { " @deprecated(reason: ${graphQlStringLiteral(it)})" }.orEmpty()
  }

  private fun String?.toSchemaDescription(indent: String): String {
    if (this == null) {
      return ""
    }
    val escaped = replace("\"\"\"", "\\\"\"\"")
    return if (!escaped.contains('\n') && !escaped.endsWith('"')) {
      "$indent\"\"\"$escaped\"\"\"\n"
    } else {
      buildString {
        appendLine("$indent\"\"\"")
        escaped.split("\n").forEach { line ->
          appendLine(if (line.isEmpty()) "" else "$indent$line")
        }
        appendLine("$indent\"\"\"")
      }
    }
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

  private fun String.toKotlinReferenceName(): String {
    return if (this == "_" || this in kotlinReservedIdentifiers) {
      "`$this`"
    } else {
      this
    }
  }
}

private val kotlinReservedIdentifiers = setOf(
  "as",
  "break",
  "class",
  "continue",
  "do",
  "else",
  "false",
  "for",
  "fun",
  "if",
  "in",
  "interface",
  "is",
  "null",
  "object",
  "package",
  "return",
  "super",
  "this",
  "throw",
  "true",
  "try",
  "typealias",
  "typeof",
  "val",
  "var",
  "when",
  "while",
  "by",
  "catch",
  "constructor",
  "delegate",
  "dynamic",
  "field",
  "file",
  "finally",
  "get",
  "import",
  "init",
  "param",
  "property",
  "receiver",
  "set",
  "setparam",
  "where",
  "actual",
  "abstract",
  "annotation",
  "companion",
  "const",
  "crossinline",
  "data",
  "enum",
  "expect",
  "external",
  "final",
  "infix",
  "inline",
  "inner",
  "internal",
  "lateinit",
  "noinline",
  "open",
  "operator",
  "out",
  "override",
  "private",
  "protected",
  "public",
  "reified",
  "sealed",
  "suspend",
  "tailrec",
  "vararg",
)
