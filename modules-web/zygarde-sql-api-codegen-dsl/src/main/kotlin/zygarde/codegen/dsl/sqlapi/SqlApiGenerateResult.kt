package zygarde.codegen.dsl.sqlapi

import com.squareup.kotlinpoet.FileSpec
import zygarde.codegen.model.WebApiGenerateResult

data class SqlApiGenerateResult(
  val dtoFileSpecs: List<FileSpec>,
  val webApiGenerateResult: WebApiGenerateResult,
  val serviceImplFileSpecs: List<FileSpec>,
)
