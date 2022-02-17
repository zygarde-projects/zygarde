package zygarde.codegen.model

import com.squareup.kotlinpoet.FileSpec

data class WebApiGenerateResult(
  val apiInterfaces: List<FileSpec>,
  val feignApiInterfaces: List<FileSpec>,
  val controllers: List<FileSpec>,
  val serviceInterfaces: List<FileSpec>,
)
