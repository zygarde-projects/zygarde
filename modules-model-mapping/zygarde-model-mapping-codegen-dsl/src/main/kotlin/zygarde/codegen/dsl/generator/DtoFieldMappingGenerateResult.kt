package zygarde.codegen.dsl.generator

import com.squareup.kotlinpoet.FileSpec

data class DtoFieldMappingGenerateResult(
  var dtoFileSpecs: List<FileSpec>,
  var modelMappingFileSpecs: List<FileSpec>,
)
