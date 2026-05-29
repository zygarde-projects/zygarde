package zygarde.codegen.model

import com.squareup.kotlinpoet.TypeName

data class CrudOperationToGenerateVo(
  val kind: CrudOperationKind,
  val functionName: String,
  val idParam: String? = null,
  val requestType: TypeName? = null,
)
