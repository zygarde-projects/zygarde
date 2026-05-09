package zygarde.codegen.dsl

import com.squareup.kotlinpoet.asClassName
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import jakarta.validation.constraints.NotBlank
import org.junit.jupiter.api.Test
import zygarde.codegen.dsl.model.internal.DtoFieldMapping
import zygarde.codegen.dsl.model.type.ForceNull
import zygarde.codegen.dsl.model.type.ValueProviderParameterType
import zygarde.codegen.meta.CodegenDtoSimple
import zygarde.codegen.value.ValueProvider

class ModelMappingSpecComprehensiveTest {
  data class TestEntity(
    val id: Int?,
    val name: String,
    val optionalName: String?,
    val owner: RefEntity?,
    val refs: List<RefEntity>,
    @field:NotBlank
    var validated: String
  )

  data class RefEntity(val id: Int?)

  enum class TestDtos : CodegenDtoSimple {
    TestDto,
    RefDto,
    ReqDto,
  }

  class ObjectProvider : ValueProvider<TestEntity, String> {
    override fun getValue(v: TestEntity): String = v.name
  }

  class FieldProvider : ValueProvider<String, String> {
    override fun getValue(v: String): String = v.uppercase()
  }

  @Test
  fun `should record every model mapping DSL variant`() {
    val mappings = mutableListOf<DtoFieldMapping>()
    val spec = ModelMappingSpec(TestDtos.TestDto, mappings)

    spec.from(TestEntity::name)
    spec.fromExtra(TestEntity::optionalName)
    spec.fromExtra<String>("computed", nullable = true)
    spec.fromRef("ownerDto", TestDtos.RefDto, nullable = true)
    spec.fromRefCollection("refDtos", TestDtos.RefDto)
    spec.fromAutoIntId(TestEntity::id)
    spec.fromObjectProvider<ObjectProvider>(TestEntity::name)
    spec.fromFieldProvider<FieldProvider>(TestEntity::optionalName)
    spec.applyTo(TestEntity::validated)
    spec.field(TestEntity::validated)
    spec.field<String>("manual", nullable = true)
    spec.fieldNullable(TestEntity::optionalName)
    spec.fieldCollection(TestEntity::refs)
    spec.fieldCollectionNullable(TestEntity::refs)
    spec.fieldRef("ownerField", TestDtos.RefDto, nullable = true)
    spec.fieldRefCollection("refFields", TestDtos.RefDto)

    mappings shouldHaveSize 16

    mappings[0].also {
      it.shouldBeInstanceOf<DtoFieldMapping.ModelToDtoFieldMappingVo>()
      it.modelField.fieldName shouldBe "name"
      it.compound shouldBe true
    }
    mappings[1].modelField.extra shouldBe true
    mappings[2].also {
      it.modelField.fieldName shouldBe "computed"
      it.modelField.fieldNullable shouldBe true
      it.modelField.extra shouldBe true
    }
    mappings[3].also {
      it.dtoRef shouldBe TestDtos.RefDto
      it.modelField.fieldName shouldBe "ownerDto"
      it.modelField.fieldNullable shouldBe true
    }
    mappings[4].also {
      it.dtoRef shouldBe TestDtos.RefDto
      it.refCollection shouldBe true
    }
    mappings[5].also {
      it.shouldBeInstanceOf<DtoFieldMapping.ModelToDtoFieldMappingVo>()
      it.forceNull shouldBe ForceNull.NOT_NULL
      it.valueProvider shouldBe zygarde.codegen.value.AutoIntIdValueProvider::class.asClassName()
      it.valueProviderParameterType shouldBe ValueProviderParameterType.OBJECT
    }
    mappings[6].also {
      it.shouldBeInstanceOf<DtoFieldMapping.ModelToDtoFieldMappingVo>()
      it.valueProvider shouldBe ObjectProvider::class.asClassName()
      it.valueProviderParameterType shouldBe ValueProviderParameterType.OBJECT
    }
    mappings[7].also {
      it.shouldBeInstanceOf<DtoFieldMapping.ModelToDtoFieldMappingVo>()
      it.valueProvider shouldBe FieldProvider::class.asClassName()
    }
    mappings[8].also {
      it.shouldBeInstanceOf<DtoFieldMapping.ModelApplyFromDtoFieldMappingVo>()
      it.additionalAnnotations shouldHaveSize 1
    }
    mappings[9].also {
      it.shouldBeInstanceOf<DtoFieldMapping.DtoFieldNoMapping>()
      it.additionalAnnotations shouldHaveSize 1
      it.compound shouldBe true
    }
    mappings[10].also {
      it.shouldBeInstanceOf<DtoFieldMapping.DtoFieldNoMapping>()
      it.modelField.fieldName shouldBe "manual"
      it.modelField.fieldNullable shouldBe true
      it.compound shouldBe true
    }
    mappings[11].forceNull shouldBe ForceNull.NULL
    mappings[12].refCollection shouldBe true
    mappings[13].also {
      it.refCollection shouldBe true
      it.forceNull shouldBe ForceNull.NULL
    }
    mappings[14].also {
      it.dtoRef shouldBe TestDtos.RefDto
      it.modelField.fieldNullable shouldBe true
    }
    mappings[15].also {
      it.dtoRef shouldBe TestDtos.RefDto
      it.refCollection shouldBe true
    }
  }
}
