package zygarde.codegen.dsl

import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import zygarde.codegen.dsl.extensions.asModelMetaField
import zygarde.codegen.dsl.model.internal.DtoFieldMapping
import zygarde.codegen.dsl.model.type.ForceNull
import zygarde.codegen.dsl.model.type.ValueProviderParameterType
import zygarde.codegen.meta.CodegenDto
import zygarde.codegen.meta.ModelMetaField
import zygarde.codegen.value.AutoIntIdValueProvider
import zygarde.codegen.value.AutoLongIdValueProvider
import zygarde.codegen.value.ValueProvider
import kotlin.reflect.KProperty1

open class ModelMappingSpec(
  val dto: CodegenDto,
  val dtoFieldMappings: MutableList<DtoFieldMapping>
) {
  fun from(vararg props: KProperty1<*, *>, dsl: (DtoFieldMapping.ModelToDtoFieldMappingVo.() -> Unit) = { }) {
    props.forEach { p ->
      DtoFieldMapping.ModelToDtoFieldMappingVo(modelField = p.asModelMetaField(), dto = dto)
        .also(dsl)
        .also { it.compound = true }
        .also(dtoFieldMappings::add)
    }
  }

  fun fromExtra(vararg props: KProperty1<*, *>, dsl: (DtoFieldMapping.ModelToDtoFieldMappingVo.() -> Unit) = {}) {
    props.forEach { p ->
      dtoFieldMappings.add(
        DtoFieldMapping.ModelToDtoFieldMappingVo(p.asModelMetaField().copy(extra = true), dto)
          .also(dsl)
          .also { it.compound = true }
      )
    }
  }

  fun fromRef(
    fieldName: String,
    dtoRef: CodegenDto,
    nullable: Boolean = false,
    dsl: (DtoFieldMapping.ModelToDtoFieldMappingVo.() -> Unit) = {}
  ) {
    dtoFieldMappings.add(
      DtoFieldMapping.ModelToDtoFieldMappingVo(
        modelField = ModelMetaField(Any::class.asTypeName(), fieldName, Any::class.asTypeName(), nullable, extra = true),
        dto = dto
      )
        .also {
          it.compound = true
          it.dtoRef = dtoRef
        }
        .also(dsl)
    )
  }

  fun fromRefCollection(
    fieldName: String,
    dtoRef: CodegenDto,
    nullable: Boolean = false,
    dsl: (DtoFieldMapping.ModelToDtoFieldMappingVo.() -> Unit) = {}
  ) {
    dtoFieldMappings.add(
      DtoFieldMapping.ModelToDtoFieldMappingVo(
        modelField = ModelMetaField(Any::class.asTypeName(), fieldName, Any::class.asTypeName(), nullable, extra = true),
        dto = dto
      )
        .also {
          it.compound = true
          it.dtoRef = dtoRef
          it.refCollection = true
        }
        .also(dsl)
    )
  }

  fun fromAutoIntId(vararg props: KProperty1<*, Int?>) {
    fromObjectProvider<AutoIntIdValueProvider>(*props) {
      this.forceNull = ForceNull.NOT_NULL
    }
  }

  fun fromAutoLongId(vararg props: KProperty1<*, Long?>) {
    fromObjectProvider<AutoLongIdValueProvider>(*props) {
      this.forceNull = ForceNull.NOT_NULL
    }
  }

  inline fun <reified P : ValueProvider<*, *>> fromObjectProvider(
    vararg props: KProperty1<*, *>,
    crossinline dsl: (DtoFieldMapping.ModelToDtoFieldMappingVo.() -> Unit) = { },
  ) {
    from(*props) {
      dsl.invoke(this)
      valueProvider = P::class.asClassName()
      valueProviderParameterType = ValueProviderParameterType.OBJECT
    }
  }

  inline fun <reified P : ValueProvider<*, *>> fromFieldProvider(
    vararg props: KProperty1<*, *>,
  ) {
    from(*props) {
      valueProvider = P::class.asClassName()
      valueProviderParameterType = ValueProviderParameterType.OBJECT
    }
  }

  fun applyTo(vararg props: KProperty1<*, *>, dsl: (DtoFieldMapping.ModelApplyFromDtoFieldMappingVo.() -> Unit) = {}) {
    props.forEach { p ->
      DtoFieldMapping.ModelApplyFromDtoFieldMappingVo(p.asModelMetaField(), dto)
        .also(dsl)
        .also(dtoFieldMappings::add)
    }
  }

  fun field(vararg props: KProperty1<*, *>, dsl: (DtoFieldMapping.DtoFieldNoMapping.() -> Unit) = {}) {
    props.forEach { p ->
      dtoFieldMappings.add(
        DtoFieldMapping.DtoFieldNoMapping(p.asModelMetaField(), dto)
          .also(dsl)
          .also { it.compound = true }
      )
    }
  }

  fun fieldNullable(vararg props: KProperty1<*, *>, dsl: (DtoFieldMapping.DtoFieldNoMapping.() -> Unit) = {}) {
    field(*props) {
      dsl(this)
      forceNull = ForceNull.NULL
    }
  }

  fun fieldCollection(vararg props: KProperty1<*, *>, dsl: (DtoFieldMapping.DtoFieldNoMapping.() -> Unit) = {}) {
    props.forEach { p ->
      dtoFieldMappings.add(
        DtoFieldMapping.DtoFieldNoMapping(p.asModelMetaField(), dto)
          .also(dsl)
          .also {
            it.compound = true
            it.refCollection = true
          }
      )
    }
  }

  fun fieldCollectionNullable(vararg props: KProperty1<*, *>, dsl: (DtoFieldMapping.DtoFieldNoMapping.() -> Unit) = {}) {
    fieldCollection(*props) {
      dsl(this)
      forceNull = ForceNull.NULL
    }
  }

  fun fieldRef(
    fieldName: String,
    dtoRef: CodegenDto,
    nullable: Boolean = false,
    dsl: (DtoFieldMapping.DtoFieldNoMapping.() -> Unit) = {}
  ) {
    dtoFieldMappings.add(
      DtoFieldMapping.DtoFieldNoMapping(
        modelField = ModelMetaField(Any::class.asTypeName(), fieldName, Any::class.asTypeName(), nullable, extra = true),
        dto = dto
      )
        .also {
          it.dtoRef = dtoRef
        }
        .also(dsl)
    )
  }

  fun fieldRefCollection(
    fieldName: String,
    dtoRef: CodegenDto,
    nullable: Boolean = false,
    dsl: (DtoFieldMapping.DtoFieldNoMapping.() -> Unit) = {}
  ) {
    dtoFieldMappings.add(
      DtoFieldMapping.DtoFieldNoMapping(
        modelField = ModelMetaField(Any::class.asTypeName(), fieldName, Any::class.asTypeName(), nullable, extra = true),
        dto = dto
      )
        .also {
          it.dtoRef = dtoRef
          it.refCollection = true
        }
        .also(dsl)
    )
  }
}
