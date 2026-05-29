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
  val applyExtensionsType: ClassName? = null,
  val patchExtensionsType: ClassName? = null,
  val operations: List<CrudOperationToGenerateVo> = emptyList(),
)
