package zygarde.codegen.model.graphql

import com.squareup.kotlinpoet.FileSpec

data class GraphQlGenerateResult(
  val controllers: List<FileSpec>,
  val serviceInterfaces: List<FileSpec>,
  val schemas: List<GraphQlSchemaGenerateResult>,
  val supportTypes: List<FileSpec> = emptyList(),
)

data class GraphQlSchemaGenerateResult(
  val fileName: String,
  val content: String,
)
