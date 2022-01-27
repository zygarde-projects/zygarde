package zygarde.codegen.model

import com.squareup.kotlinpoet.FileSpec

data class WebMvcApiGenerateResult(
  val apiInterfaces: List<FileSpec>,
  val feignApiInterfaces: List<FileSpec>,
  val webMvcControllers: List<FileSpec>,
  val serviceInterfaces: List<FileSpec>,
)
