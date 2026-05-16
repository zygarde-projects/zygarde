package zygarde.codegen.model.graphql

import com.squareup.kotlinpoet.FileSpec

data class GraphQlGenerateResult(
  val controllers: List<FileSpec>,
  val serviceInterfaces: List<FileSpec>,
  val schemas: List<GraphQlSchemaGenerateResult>,
)

data class GraphQlSchemaGenerateResult(
  val fileName: String,
  val content: String,
)
