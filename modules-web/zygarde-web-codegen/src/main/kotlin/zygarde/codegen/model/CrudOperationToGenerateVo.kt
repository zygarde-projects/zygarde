package zygarde.codegen.model

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName

data class CrudOperationToGenerateVo(
  val kind: CrudOperationKind,
  val functionName: String,
  val idParam: String? = null,
  val requestType: TypeName? = null,
  val daoMethod: String? = null,
  val notFound: CrudNotFoundToGenerateVo? = null,
  val hookType: ClassName? = null,
)
