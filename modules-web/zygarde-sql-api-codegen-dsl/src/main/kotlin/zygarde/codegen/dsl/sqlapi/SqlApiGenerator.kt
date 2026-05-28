package zygarde.codegen.dsl.sqlapi

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RequestMethod
import zygarde.codegen.generator.WebMvcApiGenerator
import zygarde.codegen.model.ApiFunctionToGenerateVo
import zygarde.codegen.model.ApiToGenerateVo
import zygarde.sql.api.ZygardeSqlExecutor
import java.io.Serializable
import javax.sql.DataSource

class SqlApiGenerator(
  private val apis: Collection<SqlApiToGenerateVo>,
) {
  fun generate(): SqlApiGenerateResult {
    return SqlApiGenerateResult(
      dtoFileSpecs = apis.flatMap { it.generateDtoFileSpecs() },
      webApiGenerateResult = WebMvcApiGenerator(apis.map { it.toWebApi() }).generateApis(),
      serviceImplFileSpecs = apis.map { it.generateServiceImplFileSpec() },
    )
  }

  private fun SqlApiToGenerateVo.generateDtoFileSpecs(): List<FileSpec> {
    val dtoTypes = queries.flatMap { query ->
      listOfNotNull(
        (query.requestName to query.params).takeIf { query.params.isNotEmpty() },
        query.responseName to query.columns,
      )
    }
    val dtoTypesByName = dtoTypes.groupBy { it.first }
    dtoTypesByName.forEach { (dtoName, definitions) ->
      val firstFields = definitions.first().second
      val hasConflict = definitions.any { (_, fields) -> fields != firstFields }
      require(!hasConflict) {
        "SQL API DTO '$dtoName' is declared with conflicting fields"
      }
    }

    return dtoTypesByName.map { (dtoName, definitions) ->
      val fields = definitions.first().second
      FileSpec.builder(config.dtoPackage, dtoName)
        .addType(generateDtoType(dtoName, fields))
        .build()
    }
  }

  private fun generateDtoType(dtoName: String, fields: List<SqlApiField>): TypeSpec {
    val constructor = FunSpec.constructorBuilder()
    val type = TypeSpec.classBuilder(dtoName)
      .addModifiers(KModifier.DATA)
      .addAnnotation(Schema::class)
      .addSuperinterface(Serializable::class)

    fields.forEach { field ->
      constructor.addParameter(
        ParameterSpec.builder(field.name, field.type)
          .also { parameter ->
            if (field.type.isNullable) {
              parameter.defaultValue("null")
            }
          }
          .build()
      )
      type.addProperty(
        PropertySpec.builder(field.name, field.type)
          .mutable(true)
          .initializer(field.name)
          .addAnnotation(field.toSchemaAnnotation())
          .build()
      )
    }

    return type.primaryConstructor(constructor.build()).build()
  }

  private fun SqlApiField.toSchemaAnnotation(): AnnotationSpec {
    return AnnotationSpec.builder(Schema::class)
      .addMember("description=%S", description)
      .addMember(
        "requiredMode=%T.RequiredMode.%L",
        Schema::class,
        if (type.isNullable) "NOT_REQUIRED" else "REQUIRED"
      )
      .build()
  }

  private fun SqlApiToGenerateVo.toWebApi(): ApiToGenerateVo {
    return ApiToGenerateVo(
      apiInterfacePackage = config.apiInterfacePackage,
      controllerPackage = config.controllerPackage,
      serviceInterfacePackage = config.serviceInterfacePackage,
      apiName = apiName,
      basePath = basePath,
      functions = queries.map { query ->
        ApiFunctionToGenerateVo(
          method = RequestMethod.GET,
          functionName = query.functionName,
          path = query.path,
          requestName = "req",
          requestType = ClassName(config.dtoPackage, query.requestName).takeIf { query.params.isNotEmpty() },
          responseType = Collection::class.asClassName(),
          responseTypeGenericArguments = listOf(ClassName(config.dtoPackage, query.responseName)),
        )
      }.toMutableList(),
      separateFeign = true,
    )
  }

  private fun SqlApiToGenerateVo.generateServiceImplFileSpec(): FileSpec {
    val serviceName = "${apiName}Service"
    val serviceImplName = "${serviceName}Impl"
    val serviceClass = ClassName(config.serviceInterfacePackage, serviceName)

    val constructor = FunSpec.constructorBuilder()
      .addParameter(
        ParameterSpec.builder("dataSource", DataSource::class)
          .addAnnotation(Autowired::class)
          .build()
      )
      .build()

    val serviceImplType = TypeSpec.classBuilder(serviceImplName)
      .addAnnotation(Service::class)
      .addSuperinterface(serviceClass)
      .primaryConstructor(constructor)
      .addProperty(
        PropertySpec.builder("dataSource", DataSource::class, KModifier.PRIVATE)
          .initializer("dataSource")
          .build()
      )
      .addProperty(
        PropertySpec.builder("executor", ZygardeSqlExecutor::class, KModifier.PRIVATE)
          .initializer("%T(dataSource)", ZygardeSqlExecutor::class)
          .build()
      )

    val companionObject = TypeSpec.companionObjectBuilder()
    queries.forEach { query ->
      val sqlConstantName = query.functionName.toSqlConstantName()
      companionObject.addProperty(
        PropertySpec.builder(sqlConstantName, String::class, KModifier.PRIVATE, KModifier.CONST)
          .initializer("%S", query.sql)
          .build()
      )
      serviceImplType.addFunction(generateServiceFunction(config, query, sqlConstantName))
    }

    serviceImplType.addType(companionObject.build())

    return FileSpec.builder(config.serviceImplPackage, serviceImplName)
      .addType(serviceImplType.build())
      .build()
  }

  private fun generateServiceFunction(
    config: SqlApiDslCodegenConfig,
    query: SqlQueryToGenerateVo,
    sqlConstantName: String
  ): FunSpec {
    val requestClass = ClassName(config.dtoPackage, query.requestName)
    val responseClass = ClassName(config.dtoPackage, query.responseName)
    val responseCollectionType = Collection::class.asClassName().parameterizedBy(responseClass)

    return FunSpec.builder(query.functionName)
      .addModifiers(KModifier.OVERRIDE)
      .also { function ->
        if (query.params.isNotEmpty()) {
          function.addParameter("req", requestClass)
        }
      }
      .returns(responseCollectionType)
      .addCode(query.toServiceFunctionBody(responseClass, sqlConstantName))
      .build()
  }

  private fun SqlQueryToGenerateVo.toServiceFunctionBody(
    responseClass: ClassName,
    sqlConstantName: String,
  ): CodeBlock {
    val body = CodeBlock.builder()
    if (params.isEmpty()) {
      body.addStatement("val params = emptyMap<String, Any?>()")
    } else {
      body.add("val params = mapOf(\n")
      body.indent()
      params.forEach { param ->
        body.addStatement("%S to req.%N,", param.name, param.name)
      }
      body.unindent()
      body.add(")\n")
    }

    body.add("return executor.query(%N, params) { row ->\n", sqlConstantName)
    body.indent()
    body.add("%T(\n", responseClass)
    body.indent()
    columns.forEachIndexed { index, column ->
      val suffix = if (index == columns.lastIndex) "" else ","
      val accessor = if (column.type.isNullable) "getNullable" else "getRequired"
      body.addStatement("%N = row.%L<%T>(%S)%L", column.name, accessor, column.type.nonNullable(), column.name, suffix)
    }
    body.unindent()
    body.add(")\n")
    body.unindent()
    body.add("}\n")
    return body.build()
  }

  private fun TypeName.nonNullable(): TypeName = copy(nullable = false)

  private fun String.toSqlConstantName(): String {
    return buildString {
      this@toSqlConstantName.forEachIndexed { index, char ->
        if (char.isUpperCase() && index > 0) {
          append('_')
        }
        append(char.uppercaseChar())
      }
      append("_SQL")
    }
  }
}
