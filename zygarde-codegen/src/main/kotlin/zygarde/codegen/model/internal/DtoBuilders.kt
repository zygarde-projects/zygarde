package zygarde.codegen.model.internal

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec

data class DtoBuilders(
  val dtoFileBuilder: FileSpec.Builder,
  val dtoClassBuilder: TypeSpec.Builder,
  val dtoConstructorBuilder: FunSpec.Builder,
)
