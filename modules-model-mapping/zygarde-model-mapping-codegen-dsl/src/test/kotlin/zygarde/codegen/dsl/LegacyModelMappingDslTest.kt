package zygarde.codegen.dsl

import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import zygarde.codegen.dsl.model.internal.DtoFieldMapping
import zygarde.codegen.dsl.model.type.ForceNull
import zygarde.codegen.dsl.model.type.ValueProviderParameterType
import zygarde.codegen.meta.CodegenDtoSimple
import zygarde.codegen.meta.ModelMetaField
import zygarde.codegen.value.AutoIntIdValueProvider
import zygarde.codegen.value.AutoLongIdValueProvider
import zygarde.codegen.value.ValueProvider

class LegacyModelMappingDslTest {
  data class Entity(val id: Int?, val longId: Long?, val name: String, val refs: List<Ref>)

  data class Ref(val id: Int?)

  enum class TestDtos : CodegenDtoSimple {
    EntityDto,
    RefDto,
    ReqDto,
  }

  class ObjectProvider : ValueProvider<Entity, String> {
    override fun getValue(v: Entity): String = v.name
  }

  class FieldProvider : ValueProvider<String, String> {
    override fun getValue(v: String): String = v.uppercase()
  }

  private fun field(name: String, nullable: Boolean = false) = ModelMetaField(
    modelClass = Entity::class.asClassName(),
    fieldName = name,
    fieldClass = String::class.asTypeName(),
    fieldNullable = nullable
  )

  @Test
  fun `ModelToDtoDsl should collect normal extra provider and ref mappings`() {
    val dsl = ModelToDtoDsl(Entity::class, TestDtos.EntityDto)
    val id = field("id", nullable = true).copy(fieldClass = Int::class.asTypeName())
    val longId = field("longId", nullable = true).copy(fieldClass = Long::class.asTypeName())
    val name = field("name")

    dsl.from(name) { comment = "Name" }
    dsl.fieldExtra(field("extra"))
    dsl.fromAutoIntId(id)
    dsl.fromAutoLongId(longId)
    dsl.fromObjectProvider<ObjectProvider>(name)
    dsl.fromFieldProvider<FieldProvider>(name)
    dsl.ref("owner", TestDtos.RefDto, nullable = true)
    dsl.refCollection("refs", TestDtos.RefDto)

    dsl.dtoFieldMappings shouldHaveSize 8
    dsl.dtoFieldMappings[0].also {
      it.shouldBeInstanceOf<DtoFieldMapping.ModelToDtoFieldMappingVo>()
      it.comment shouldBe "Name"
    }
    dsl.dtoFieldMappings[1].modelField.extra shouldBe true
    dsl.dtoFieldMappings[2].also {
      it.shouldBeInstanceOf<DtoFieldMapping.ModelToDtoFieldMappingVo>()
      it.valueProvider shouldBe AutoIntIdValueProvider::class.asClassName()
      it.valueProviderParameterType shouldBe ValueProviderParameterType.OBJECT
    }
    dsl.dtoFieldMappings[3].also {
      it.shouldBeInstanceOf<DtoFieldMapping.ModelToDtoFieldMappingVo>()
      it.valueProvider shouldBe AutoLongIdValueProvider::class.asClassName()
    }
    dsl.dtoFieldMappings[4].also {
      it.shouldBeInstanceOf<DtoFieldMapping.ModelToDtoFieldMappingVo>()
      it.valueProvider shouldBe ObjectProvider::class.asClassName()
    }
    dsl.dtoFieldMappings[5].also {
      it.shouldBeInstanceOf<DtoFieldMapping.ModelToDtoFieldMappingVo>()
      it.valueProvider shouldBe FieldProvider::class.asClassName()
    }
    dsl.dtoFieldMappings[6].also {
      it.dtoRef shouldBe TestDtos.RefDto
      it.modelField.fieldNullable shouldBe true
    }
    dsl.dtoFieldMappings[7].also {
      it.dtoRef shouldBe TestDtos.RefDto
      it.refCollection shouldBe true
    }
  }

  @Test
  fun `DtoApplyToModelDsl and ModelFieldDsl should collect apply field and ref mappings`() {
    val dsl = DtoApplyToModelDsl(Entity::class, TestDtos.ReqDto)
    val name = field("name")
    val refs = field("refs")

    dsl.applyTo(name)
    dsl.field(name)
    dsl.fieldNullable(name)
    dsl.fieldCollection(refs)
    dsl.fieldCollectionNullable(refs)
    dsl.ref("owner", TestDtos.RefDto, nullable = true)
    dsl.refCollection("owners", TestDtos.RefDto)

    dsl.dtoFieldMappings shouldHaveSize 7
    dsl.dtoFieldMappings[0].shouldBeInstanceOf<DtoFieldMapping.ModelApplyFromDtoFieldMappingVo>()
    dsl.dtoFieldMappings[1].shouldBeInstanceOf<DtoFieldMapping.DtoFieldNoMapping>()
    dsl.dtoFieldMappings[2].forceNull shouldBe ForceNull.NULL
    dsl.dtoFieldMappings[3].refCollection shouldBe true
    dsl.dtoFieldMappings[4].also {
      it.refCollection shouldBe true
      it.forceNull shouldBe ForceNull.NULL
    }
    dsl.dtoFieldMappings[5].also {
      it.dtoRef shouldBe TestDtos.RefDto
      it.modelField.fieldNullable shouldBe true
    }
    dsl.dtoFieldMappings[6].also {
      it.dtoRef shouldBe TestDtos.RefDto
      it.refCollection shouldBe true
    }
  }

  @Test
  fun `ModelMappingCodegenSpec should invoke single DTO groups and sealed interfaces`() {
    val spec = object : ModelMappingCodegenSpec({
      TestDtos.EntityDto {
        from(Entity::name)
      }
      group(TestDtos.EntityDto, TestDtos.ReqDto) {
        field(Entity::name)
      }
      sealedInterface("EntityResult") {
        subtype("entity", TestDtos.EntityDto)
      }
    }) {}

    spec.dtoFieldMappings shouldHaveSize 3
    spec.dtoFieldMappings.map { it.dto } shouldBe listOf(TestDtos.EntityDto, TestDtos.EntityDto, TestDtos.ReqDto)
    spec.sealedInterfaces shouldHaveSize 1
    spec.sealedInterfaces.first().name shouldBe "EntityResult"
    spec.execute()
  }
}
