package zygarde.codegen.generator

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
import org.springframework.stereotype.Controller
import zygarde.codegen.model.graphql.GraphQlApiToGenerateVo
import zygarde.codegen.model.graphql.GraphQlArgumentToGenerateVo
import zygarde.codegen.model.graphql.GraphQlFieldToGenerateVo
import zygarde.codegen.model.graphql.GraphQlGenerateResult
import zygarde.codegen.model.graphql.GraphQlFunctionToGenerateVo
import zygarde.codegen.model.graphql.GraphQlOperation
import zygarde.codegen.model.graphql.GraphQlSchemaGenerateResult
import zygarde.codegen.model.graphql.GraphQlTypeDefinitionKind

class GraphQlApiGenerator(
  private val apis: Collection<GraphQlApiToGenerateVo>
) {
  private val controllerFileSpecBuilderMap = mutableMapOf<String, FileSpec.Builder>()
  private val controllerBuilderMap = mutableMapOf<String, TypeSpec.Builder>()
  private val serviceInterfaceFileSpecBuilderMap = mutableMapOf<String, FileSpec.Builder>()
  private val serviceInterfaceBuilderMap = mutableMapOf<String, TypeSpec.Builder>()

  private val beanFunc = MemberName("zygarde.core.di.DiServiceContext", "bean")

  fun generateApis(): GraphQlGenerateResult {
    apis.forEach { it.generate() }

    controllerFileSpecBuilderMap.forEach { (controllerName, fileSpecBuilder) ->
      controllerBuilderMap[controllerName]?.build()?.let(fileSpecBuilder::addType)
    }
    serviceInterfaceFileSpecBuilderMap.forEach { (serviceName, fileSpecBuilder) ->
      serviceInterfaceBuilderMap[serviceName]?.build()?.let(fileSpecBuilder::addType)
    }

    return GraphQlGenerateResult(
      controllers = controllerFileSpecBuilderMap.values.map { it.build() },
      serviceInterfaces = serviceInterfaceFileSpecBuilderMap.values.map { it.build() },
      schemas = apis.map { it.toSchemaGenerateResult() },
    )
  }

  private fun GraphQlApiToGenerateVo.generate() {
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
        .addAnnotation(
          when (function.operation) {
            GraphQlOperation.QUERY -> QueryMapping::class
            GraphQlOperation.MUTATION -> MutationMapping::class
          }
        )
        .returns(function.kotlinResponseType())

      val paramsToCallServiceInterface = mutableListOf<String>()
      function.arguments.forEach { argument ->
        val parameter = argument.toParameterSpec()
        serviceFuncBuilder.addParameter(parameter)
        controllerFuncBuilder.addParameter(
          parameter.toBuilder()
            .addAnnotation(Argument::class)
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
    return ParameterSpec.builder(name, type.copy(nullable = nullable)).build()
  }

  private fun zygarde.codegen.model.graphql.GraphQlFunctionToGenerateVo.kotlinResponseType(): TypeName {
    return if (responseCollection) {
      Collection::class.asTypeName().parameterizedBy(responseType)
    } else {
      responseType
    }
  }

  private fun GraphQlApiToGenerateVo.toSchemaGenerateResult(): GraphQlSchemaGenerateResult {
    val schema = buildString {
      appendOperationType(functions.filter { it.operation == GraphQlOperation.QUERY }, "Query")
      appendOperationType(functions.filter { it.operation == GraphQlOperation.MUTATION }, "Mutation")
      typeDefinitions.forEach { typeDefinition ->
        appendLine()
        appendLine("${typeDefinition.kind.schemaKeyword()} ${typeDefinition.name} {")
        typeDefinition.fields.forEach { field ->
          appendLine("  ${field.name}: ${field.toSchemaType()}")
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
    typeName: String
  ) {
    if (functions.isEmpty()) {
      return
    }

    if (isNotEmpty()) {
      appendLine()
    }
    appendLine("type $typeName {")
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
      "${argument.name}: ${argument.graphQlType}${if (argument.nullable) "" else "!"}"
    }
  }

  private fun GraphQlFunctionToGenerateVo.toSchemaResponseType(): String {
    return if (responseCollection) {
      "[$responseGraphQlType!]!"
    } else {
      "$responseGraphQlType!"
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

  private fun GraphQlTypeDefinitionKind.schemaKeyword(): String {
    return when (this) {
      GraphQlTypeDefinitionKind.TYPE -> "type"
      GraphQlTypeDefinitionKind.INPUT -> "input"
    }
  }
}
