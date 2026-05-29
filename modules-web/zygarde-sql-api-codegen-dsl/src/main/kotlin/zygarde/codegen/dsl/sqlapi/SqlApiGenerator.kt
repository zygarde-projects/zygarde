package zygarde.codegen.dsl.sqlapi

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RequestMethod
import zygarde.codegen.generator.WebMvcApiGenerator
import zygarde.codegen.model.ApiFunctionToGenerateVo
import zygarde.codegen.model.ApiToGenerateVo
import zygarde.core.di.DiServiceContext
import zygarde.data.api.PageDto
import zygarde.sql.api.SqlApiContextParamResolver
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
    val queryDtoTypes = queries.flatMap { query ->
      listOfNotNull(
        (query.requestName to query.requestDtoParams()).takeIf { it.second.isNotEmpty() },
        query.responseName to query.responseDtoFields(),
      )
    }
    val commandDtoTypes = commands.flatMap { command ->
      listOfNotNull(
        (command.requestName to command.requestDtoParams()).takeIf { it.second.isNotEmpty() },
        command.generatedKey?.let { it.responseName to listOf(it.field) },
      )
    }
    val dtoTypes = queryDtoTypes + commandDtoTypes
    val dtoTypesByName = dtoTypes.groupBy { it.first }
    dtoTypesByName.forEach { (dtoName, definitions) ->
      val firstFields = definitions.first().second
      val hasConflict = definitions.any { (_, fields) -> fields.map { it.toDtoField() } != firstFields.map { it.toDtoField() } }
      require(!hasConflict) {
        "SQL API DTO '$dtoName' is declared with conflicting fields"
      }
    }

    return dtoTypesByName.map { (dtoName, definitions) ->
      val fields = definitions.first().second
      FileSpec.builder(config.dtoPackage, dtoName)
        .addType(generateDtoType(dtoName, fields.map { it.toDtoField() }))
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
      functions = (
        queries.map { query ->
          ApiFunctionToGenerateVo(
            method = RequestMethod.GET,
            functionName = query.functionName,
            path = query.path,
            pathVariables = query.pathVariables(),
            requestParams = query.requestParams(),
            requestName = "req",
            requestType = ClassName(config.dtoPackage, query.requestName).takeIf { query.requestDtoParams().isNotEmpty() },
            responseType = query.responseType(config),
            responseTypeGenericArguments = query.responseTypeGenericArguments(config),
          )
        } + commands.map { command ->
          ApiFunctionToGenerateVo(
            method = command.method,
            functionName = command.functionName,
            path = command.path,
            pathVariables = command.pathVariables(),
            requestParams = command.requestParams(),
            requestName = "req",
            requestType = ClassName(config.dtoPackage, command.requestName).takeIf { command.requestDtoParams().isNotEmpty() },
            responseType = command.responseType(config),
            responseStatus = command.responseStatus(),
          )
        }
      ).toMutableList(),
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
          .also { parameter ->
            database.dataSourceQualifier?.let { qualifier ->
              parameter.addAnnotation(
                AnnotationSpec.builder(Qualifier::class)
                  .addMember("%S", qualifier)
                  .build()
              )
            }
          }
          .build()
      )
    val providerTypes = queries.flatMap { it.providerFields.map { providerField -> providerField.providerType } }.distinct()
    providerTypes.forEach { providerType ->
      constructor.addParameter(
        ParameterSpec.builder(providerType.providerPropertyName(), providerType)
          .addAnnotation(Autowired::class)
          .build()
      )
    }
    val constructorSpec = constructor.build()

    val serviceImplType = TypeSpec.classBuilder(serviceImplName)
      .addAnnotation(Service::class)
      .addSuperinterface(serviceClass)
      .primaryConstructor(constructorSpec)
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
    providerTypes.forEach { providerType ->
      serviceImplType.addProperty(
        PropertySpec.builder(providerType.providerPropertyName(), providerType, KModifier.PRIVATE)
          .initializer(providerType.providerPropertyName())
          .build()
      )
    }

    val companionObject = TypeSpec.companionObjectBuilder()
    queries.forEach { query ->
      val sqlConstantName = query.functionName.toSqlConstantName()
      companionObject.addProperty(
        PropertySpec.builder(sqlConstantName, String::class, KModifier.PRIVATE, KModifier.CONST)
          .initializer("%S", query.sql)
          .build()
      )
      val countSqlConstantName = query.page?.let {
        "${query.functionName.toSqlConstantName().removeSuffix("_SQL")}_COUNT_SQL"
      }
      if (countSqlConstantName != null) {
        companionObject.addProperty(
          PropertySpec.builder(countSqlConstantName, String::class, KModifier.PRIVATE, KModifier.CONST)
            .initializer("%S", query.page.countSql)
            .build()
        )
      }
      if (query.providerFields.isNotEmpty()) {
        serviceImplType.addType(query.generateRowType())
      }
      serviceImplType.addFunction(
        generateServiceFunction(
          config,
          query,
          sqlConstantName,
          countSqlConstantName,
          query.effectiveTransactionPolicy(this),
          database.transactionManagerQualifier,
        )
      )
    }
    commands.forEach { command ->
      val sqlConstantName = command.functionName.toSqlConstantName()
      companionObject.addProperty(
        PropertySpec.builder(sqlConstantName, String::class, KModifier.PRIVATE, KModifier.CONST)
          .initializer("%S", command.sql)
          .build()
      )
      serviceImplType.addFunction(
        generateCommandServiceFunction(
          config,
          command,
          sqlConstantName,
          command.effectiveTransactionPolicy(this),
          database.transactionManagerQualifier,
        )
      )
    }

    serviceImplType.addType(companionObject.build())

    return FileSpec.builder(config.serviceImplPackage, serviceImplName)
      .addType(serviceImplType.build())
      .build()
  }

  private fun generateServiceFunction(
    config: SqlApiDslCodegenConfig,
    query: SqlQueryToGenerateVo,
    sqlConstantName: String,
    countSqlConstantName: String?,
    transactionPolicy: SqlApiTransactionPolicy,
    transactionManagerQualifier: String?,
  ): FunSpec {
    val requestClass = ClassName(config.dtoPackage, query.requestName)
    val responseClass = ClassName(config.dtoPackage, query.responseName)

    return FunSpec.builder(query.functionName)
      .addModifiers(KModifier.OVERRIDE)
      .addTransactionAnnotation(transactionPolicy, transactionManagerQualifier)
      .also { function ->
        query.pathVariables().forEach { (name, type) ->
          function.addParameter(name, type)
        }
        query.requestParams().forEach { (name, type) ->
          function.addParameter(name, type)
        }
        if (query.requestDtoParams().isNotEmpty()) {
          function.addParameter("req", requestClass)
        }
      }
      .returns(query.serviceReturnType(config))
      .addCode(query.toServiceFunctionBody(responseClass, sqlConstantName, countSqlConstantName))
      .build()
  }

  private fun SqlQueryToGenerateVo.toServiceFunctionBody(
    responseClass: ClassName,
    sqlConstantName: String,
    countSqlConstantName: String?,
  ): CodeBlock {
    if (providerFields.isNotEmpty()) {
      return toProviderServiceFunctionBody(responseClass, sqlConstantName, countSqlConstantName)
    }

    val body = CodeBlock.builder()
    if (params.isEmpty()) {
      body.addStatement("val params = emptyMap<String, Any?>()")
    } else {
      body.add("val params = mapOf(\n")
      body.indent()
      params.forEach { param ->
        body.addStatement("%S to %L,", param.name, param.valueExpression())
      }
      page?.let {
        body.addStatement(
          "%S to (%L * %L),",
          it.offsetParamName,
          paramValueExpression(it.pageParamName),
          paramValueExpression(it.pageSizeParamName),
        )
      }
      body.unindent()
      body.add(")\n")
    }

    when (resultShape) {
      SqlQueryResultShape.LIST -> body.add("return executor.query(%N, params) { row ->\n", sqlConstantName)
      SqlQueryResultShape.ONE -> body.add("return executor.queryOne(%N, params) { row ->\n", sqlConstantName)
      SqlQueryResultShape.ONE_NULLABLE -> body.add("return executor.queryOneOrNull(%N, params) { row ->\n", sqlConstantName)
      SqlQueryResultShape.PAGE -> body.add("val items = executor.query(%N, params) { row ->\n", sqlConstantName)
    }
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
    if (resultShape == SqlQueryResultShape.PAGE) {
      val page = requireNotNull(page)
      val countSql = requireNotNull(countSqlConstantName)
      body.add("val totalCount = executor.queryOne(%N, params) { row ->\n", countSql)
      body.indent()
      body.addStatement("row.getRequired<Long>(%S)", page.countColumnName)
      body.unindent()
      body.add("}\n")
      body.addStatement(
        "val totalPages = if (%L <= 0) 0 else ((totalCount + %L - 1) / %L).toInt()",
        paramValueExpression(page.pageSizeParamName),
        paramValueExpression(page.pageSizeParamName),
        paramValueExpression(page.pageSizeParamName),
      )
      body.addStatement("return %T(%L, totalPages, items, totalCount)", PageDto::class, paramValueExpression(page.pageParamName))
    }
    return body.build()
  }

  private fun SqlQueryToGenerateVo.toProviderServiceFunctionBody(
    responseClass: ClassName,
    sqlConstantName: String,
    countSqlConstantName: String?,
  ): CodeBlock {
    val body = CodeBlock.builder()
    if (params.isEmpty()) {
      body.addStatement("val params = emptyMap<String, Any?>()")
    } else {
      body.add("val params = mapOf(\n")
      body.indent()
      params.forEach { param ->
        body.addStatement("%S to %L,", param.name, param.valueExpression())
      }
      page?.let {
        body.addStatement(
          "%S to (%L * %L),",
          it.offsetParamName,
          paramValueExpression(it.pageParamName),
          paramValueExpression(it.pageSizeParamName),
        )
      }
      body.unindent()
      body.add(")\n")
    }
    body.add("val rows = executor.query(%N, params) { row ->\n", sqlConstantName)
    body.indent()
    body.add("%T(\n", rowClassName())
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

    providerFields.forEach { providerField ->
      body.addStatement(
        "val %N = rows.mapNotNull·{ it.%N }.distinct()",
        providerField.keysVariableName(),
        providerField.keyColumnName,
      )
      body.addStatement(
        "val %N = %N.load(%N)",
        providerField.valuesVariableName(),
        providerField.providerType.providerPropertyName(),
        providerField.keysVariableName(),
      )
    }

    body.add("val items = rows.map { row ->\n")
    body.indent()
    body.add("%T(\n", responseClass)
    body.indent()
    responseDtoFields().forEachIndexed { index, field ->
      val suffix = if (index == responseDtoFields().lastIndex) "" else ","
      val expression = providerFields.firstOrNull { it.name == field.name }?.providerValueExpression("row") ?: "row.${field.name}"
      body.addStatement("%N = %L%L", field.name, expression, suffix)
    }
    body.unindent()
    body.add(")\n")
    body.unindent()
    body.add("}\n")

    when (resultShape) {
      SqlQueryResultShape.LIST -> body.addStatement("return items")
      SqlQueryResultShape.ONE -> body.addStatement("return items.single()")
      SqlQueryResultShape.ONE_NULLABLE -> body.addStatement("return items.singleOrNull()")
      SqlQueryResultShape.PAGE -> {
        val page = requireNotNull(page)
        val countSql = requireNotNull(countSqlConstantName)
        body.add("val totalCount = executor.queryOne(%N, params) { row ->\n", countSql)
        body.indent()
        body.addStatement("row.getRequired<Long>(%S)", page.countColumnName)
        body.unindent()
        body.add("}\n")
        body.addStatement(
          "val totalPages = if (%L <= 0) 0 else ((totalCount + %L - 1) / %L).toInt()",
          paramValueExpression(page.pageSizeParamName),
          paramValueExpression(page.pageSizeParamName),
          paramValueExpression(page.pageSizeParamName),
        )
        body.addStatement("return %T(%L, totalPages, items, totalCount)", PageDto::class, paramValueExpression(page.pageParamName))
      }
    }
    return body.build()
  }

  private fun generateCommandServiceFunction(
    config: SqlApiDslCodegenConfig,
    command: SqlCommandToGenerateVo,
    sqlConstantName: String,
    transactionPolicy: SqlApiTransactionPolicy,
    transactionManagerQualifier: String?,
  ): FunSpec {
    val requestClass = ClassName(config.dtoPackage, command.requestName)
    return FunSpec.builder(command.functionName)
      .addModifiers(KModifier.OVERRIDE)
      .addTransactionAnnotation(transactionPolicy, transactionManagerQualifier)
      .also { function ->
        command.pathVariables().forEach { (name, type) ->
          function.addParameter(name, type)
        }
        command.requestParams().forEach { (name, type) ->
          function.addParameter(name, type)
        }
        if (command.requestDtoParams().isNotEmpty()) {
          function.addParameter("req", requestClass)
        }
        command.serviceReturnType(config)?.let {
          function.returns(it)
        }
      }
      .addCode(command.toServiceFunctionBody(config, sqlConstantName))
      .build()
  }

  private fun SqlCommandToGenerateVo.toServiceFunctionBody(
    config: SqlApiDslCodegenConfig,
    sqlConstantName: String,
  ): CodeBlock {
    val body = CodeBlock.builder()
    body.addParamsMap(params)
    when (resultShape) {
      SqlCommandResultShape.AFFECTED_ROWS -> {
        body.addStatement("return executor.execute(%N, params)", sqlConstantName)
      }
      SqlCommandResultShape.NO_CONTENT -> {
        body.addStatement("executor.execute(%N, params)", sqlConstantName)
      }
      SqlCommandResultShape.GENERATED_KEY -> {
        val generatedKey = requireNotNull(generatedKey)
        val responseClass = ClassName(config.dtoPackage, generatedKey.responseName)
        body.addStatement(
          "val key = executor.insertAndReturnKey<%T>(%N, params, %S)",
          generatedKey.field.type.nonNullable(),
          sqlConstantName,
          generatedKey.keyColumnName,
        )
        body.addStatement("return %T(%N = key)", responseClass, generatedKey.field.name)
      }
    }
    return body.build()
  }

  private fun CodeBlock.Builder.addParamsMap(params: List<SqlApiField>) {
    if (params.isEmpty()) {
      addStatement("val params = emptyMap<String, Any?>()")
    } else {
      add("val params = mapOf(\n")
      indent()
      params.forEach { param ->
        addStatement("%S to %L,", param.name, param.valueExpression())
      }
      unindent()
      add(")\n")
    }
  }

  private fun SqlApiField.valueExpression(): CodeBlock {
    return when (source) {
      SqlApiParamSource.PATH,
      SqlApiParamSource.QUERY -> CodeBlock.of("%N", name)
      SqlApiParamSource.AUTO,
      SqlApiParamSource.BODY -> CodeBlock.of("req.%N", name)
      SqlApiParamSource.CONTEXT -> contextValueExpression()
    }
  }

  private fun SqlApiField.contextValueExpression(): CodeBlock {
    return when (val valueSource = requireNotNull(contextValueSource) { "Context parameter '$name' must declare a value source" }) {
      is SqlApiContextValueSource.ResolverByType -> CodeBlock.of(
        "%M<%T>().resolve(%S)",
        BEAN,
        valueSource.resolverType,
        name,
      )
      is SqlApiContextValueSource.ResolverByName -> CodeBlock.of(
        "(%T.ctx.getBean(%S) as %T<*>).resolve(%S) as %T",
        DiServiceContext::class,
        valueSource.beanName,
        SqlApiContextParamResolver::class,
        name,
        type,
      )
    }
  }

  private fun SqlQueryToGenerateVo.paramValueExpression(name: String): CodeBlock {
    return params.first { it.name == name }.valueExpression()
  }

  private fun SqlApiField.toDtoField(): SqlApiField {
    return copy(source = SqlApiParamSource.AUTO, hidden = false)
  }

  private fun SqlQueryToGenerateVo.responseDtoFields(): List<SqlApiField> {
    return columns.filterNot { it.hidden } + providerFields.map {
      SqlApiField(it.name, it.valueType.copy(nullable = it.nullable), it.description)
    }
  }

  private fun SqlQueryToGenerateVo.rowClassName(): ClassName {
    return ClassName("", functionName.replaceFirstChar { it.uppercase() } + "Row")
  }

  private fun SqlQueryToGenerateVo.generateRowType(): TypeSpec {
    val constructor = FunSpec.constructorBuilder()
    val type = TypeSpec.classBuilder(rowClassName())
      .addModifiers(KModifier.PRIVATE, KModifier.DATA)

    columns.forEach { column ->
      constructor.addParameter(column.name, column.type)
      type.addProperty(
        PropertySpec.builder(column.name, column.type)
          .initializer(column.name)
          .build()
      )
    }

    return type.primaryConstructor(constructor.build()).build()
  }

  private fun ClassName.providerPropertyName(): String {
    return simpleName.replaceFirstChar { it.lowercase() }
  }

  private fun SqlApiDataProviderField.keysVariableName(): String {
    return "${name}Keys"
  }

  private fun SqlApiDataProviderField.valuesVariableName(): String {
    return "${name}Values"
  }

  private fun SqlApiDataProviderField.providerValueExpression(rowRef: String): String {
    val keyExpression = "$rowRef.$keyColumnName"
    return if (nullable) {
      "$keyExpression?.let { ${valuesVariableName()}[it] }"
    } else {
      "$keyExpression?.let { key -> ${valuesVariableName()}[key] ?: throw IllegalStateException(" +
        "\"Data provider ${providerType.simpleName} did not return value for field '$name' and key '\$key'\") } " +
        "?: throw IllegalStateException(\"Data provider ${providerType.simpleName} did not return value for field '$name' and key 'null'\")"
    }
  }

  private fun SqlQueryToGenerateVo.requestDtoParams(): List<SqlApiField> {
    return params.filter { it.source == SqlApiParamSource.AUTO || it.source == SqlApiParamSource.BODY }
  }

  private fun SqlQueryToGenerateVo.pathVariables(): Map<String, TypeName> {
    return params
      .filter { it.source == SqlApiParamSource.PATH }
      .associate { it.name to it.type }
  }

  private fun SqlQueryToGenerateVo.requestParams(): Map<String, TypeName> {
    return params
      .filter { it.source == SqlApiParamSource.QUERY }
      .associate { it.name to it.type }
  }

  private fun SqlCommandToGenerateVo.requestDtoParams(): List<SqlApiField> {
    return params.filter { it.source == SqlApiParamSource.AUTO || it.source == SqlApiParamSource.BODY }
  }

  private fun SqlCommandToGenerateVo.pathVariables(): Map<String, TypeName> {
    return params
      .filter { it.source == SqlApiParamSource.PATH }
      .associate { it.name to it.type }
  }

  private fun SqlCommandToGenerateVo.requestParams(): Map<String, TypeName> {
    return params
      .filter { it.source == SqlApiParamSource.QUERY }
      .associate { it.name to it.type }
  }

  private fun SqlQueryToGenerateVo.responseType(config: SqlApiDslCodegenConfig): TypeName {
    val responseClass = ClassName(config.dtoPackage, responseName)
    return when (resultShape) {
      SqlQueryResultShape.LIST -> Collection::class.asClassName()
      SqlQueryResultShape.ONE -> responseClass
      SqlQueryResultShape.ONE_NULLABLE -> responseClass.copy(nullable = true)
      SqlQueryResultShape.PAGE -> PageDto::class.asClassName()
    }
  }

  private fun SqlQueryToGenerateVo.responseTypeGenericArguments(config: SqlApiDslCodegenConfig): List<TypeName> {
    val responseClass = ClassName(config.dtoPackage, responseName)
    return when (resultShape) {
      SqlQueryResultShape.LIST,
      SqlQueryResultShape.PAGE -> listOf(responseClass)
      SqlQueryResultShape.ONE,
      SqlQueryResultShape.ONE_NULLABLE -> emptyList()
    }
  }

  private fun SqlQueryToGenerateVo.serviceReturnType(config: SqlApiDslCodegenConfig): TypeName {
    val responseClass = ClassName(config.dtoPackage, responseName)
    return when (resultShape) {
      SqlQueryResultShape.LIST -> Collection::class.asClassName().parameterizedBy(responseClass)
      SqlQueryResultShape.ONE -> responseClass
      SqlQueryResultShape.ONE_NULLABLE -> responseClass.copy(nullable = true)
      SqlQueryResultShape.PAGE -> PageDto::class.asClassName().parameterizedBy(responseClass)
    }
  }

  private fun SqlCommandToGenerateVo.responseType(config: SqlApiDslCodegenConfig): TypeName? {
    return when (resultShape) {
      SqlCommandResultShape.AFFECTED_ROWS -> Int::class.asTypeName()
      SqlCommandResultShape.NO_CONTENT -> null
      SqlCommandResultShape.GENERATED_KEY -> ClassName(config.dtoPackage, requireNotNull(generatedKey).responseName)
    }
  }

  private fun SqlCommandToGenerateVo.serviceReturnType(config: SqlApiDslCodegenConfig): TypeName? {
    return responseType(config)
  }

  private fun SqlQueryToGenerateVo.effectiveTransactionPolicy(api: SqlApiToGenerateVo): SqlApiTransactionPolicy {
    return transactionPolicy ?: api.transactionPolicy ?: SqlApiTransactionPolicy.NONE
  }

  private fun SqlCommandToGenerateVo.effectiveTransactionPolicy(api: SqlApiToGenerateVo): SqlApiTransactionPolicy {
    return transactionPolicy ?: api.transactionPolicy ?: SqlApiTransactionPolicy.READ_WRITE
  }

  private fun SqlCommandToGenerateVo.responseStatus(): HttpStatus? {
    return when (resultShape) {
      SqlCommandResultShape.NO_CONTENT -> HttpStatus.NO_CONTENT
      SqlCommandResultShape.GENERATED_KEY -> HttpStatus.CREATED
      SqlCommandResultShape.AFFECTED_ROWS -> null
    }
  }

  private fun FunSpec.Builder.addTransactionAnnotation(
    transactionPolicy: SqlApiTransactionPolicy,
    transactionManagerQualifier: String?,
  ): FunSpec.Builder {
    when (transactionPolicy) {
      SqlApiTransactionPolicy.READ_ONLY -> addAnnotation(
        AnnotationSpec.builder(Transactional::class)
          .also { annotation ->
            transactionManagerQualifier?.let {
              annotation.addMember("transactionManager = %S", it)
            }
          }
          .addMember("readOnly = true")
          .build()
      )
      SqlApiTransactionPolicy.READ_WRITE -> addAnnotation(
        AnnotationSpec.builder(Transactional::class)
          .also { annotation ->
            transactionManagerQualifier?.let {
              annotation.addMember("transactionManager = %S", it)
            }
          }
          .build()
      )
      SqlApiTransactionPolicy.NONE -> Unit
    }
    return this
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

  private companion object {
    private val BEAN = MemberName("zygarde.core.di.DiServiceContext", "bean")
  }
}
