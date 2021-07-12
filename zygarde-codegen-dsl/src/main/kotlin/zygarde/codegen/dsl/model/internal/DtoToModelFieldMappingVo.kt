package zygarde.codegen.dsl.model.internal

import zygarde.codegen.dsl.model.type.ForceNull
import zygarde.codegen.dsl.model.type.ValueProviderParameterType
import zygarde.codegen.meta.CodegenDto
import zygarde.codegen.meta.ModelMetaField
import zygarde.codegen.value.ValueProvider
import kotlin.reflect.KClass

data class DtoToModelFieldMappingVo(
  var modelField: ModelMetaField<*, *>,
  var dto: CodegenDto,
  var comment: String = "",
  var dtoRef: CodegenDto? = null,
  var dtoRefClass: KClass<*>? = null,
  var refCollection: Boolean = false,
  var forceNull: ForceNull = ForceNull.NONE,
  var valueProvider: KClass<out ValueProvider<*, *>>? = null,
  var valueProviderParameterType: ValueProviderParameterType = ValueProviderParameterType.FIELD,
)
