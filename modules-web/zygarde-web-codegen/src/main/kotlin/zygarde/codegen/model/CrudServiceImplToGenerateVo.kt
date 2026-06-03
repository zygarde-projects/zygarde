package zygarde.codegen.model

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName

data class CrudServiceImplToGenerateVo(
  val serviceName: String,
  val entityType: TypeName,
  val idType: TypeName,
  val daoType: TypeName,
  val daoPropertyName: String,
  val dtoBuilderType: ClassName,
  val dtoAssemblerType: ClassName = ClassName(
    dtoBuilderType.packageName,
    dtoBuilderType.simpleName.removeSuffix("Builder") + "Assembler"
  ),
  val applyExtensionsType: ClassName? = null,
  val patchExtensionsType: ClassName? = null,
  val transactional: CrudTransactionalToGenerateVo? = null,
  val notFound: CrudNotFoundToGenerateVo? = null,
  val softDeleteTimestamp: CrudSoftDeleteTimestampToGenerateVo? = null,
  val operations: List<CrudOperationToGenerateVo> = emptyList(),
)

data class CrudTransactionalToGenerateVo(
  val transactionManager: String? = null,
)

data class CrudNotFoundToGenerateVo(
  val errorCodeType: ClassName,
  val errorCodeName: String,
)

data class CrudSoftDeleteTimestampToGenerateVo(
  val fieldName: String,
)
