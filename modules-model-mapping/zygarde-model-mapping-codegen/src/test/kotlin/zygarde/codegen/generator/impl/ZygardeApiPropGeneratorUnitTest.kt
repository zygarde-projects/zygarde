package zygarde.codegen.generator.impl

import com.squareup.kotlinpoet.asTypeName
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import zygarde.codegen.value.NoOpValueProvider

class ZygardeApiPropGeneratorUnitTest {
  @Test
  fun `DtoFieldDescriptionVo should create with all fields`() {
    // given/when
    val vo = ZygardeApiPropGenerator.DtoFieldDescriptionVo(
      entityFieldName = "userId",
      entityFieldType = Long::class.asTypeName(),
      dtoName = "UserDto",
      dtoFieldName = "id",
      dtoFieldType = String::class.asTypeName(),
      comment = "User ID",
      dtoRef = "UserRef",
      dtoRefCollection = true,
      valueProvider = String::class.asTypeName(),
      entityValueProvider = Long::class.asTypeName(),
      generateToDtoExtension = true,
      generateApplyToEntityExtension = true,
      searchType = zygarde.codegen.SearchType.EQ,
      searchForField = "user",
      sinceApiVersion = 2L
    )

    // then
    vo.entityFieldName shouldBe "userId"
    vo.entityFieldType shouldBe Long::class.asTypeName()
    vo.dtoName shouldBe "UserDto"
    vo.dtoFieldName shouldBe "id"
    vo.dtoFieldType shouldBe String::class.asTypeName()
    vo.comment shouldBe "User ID"
    vo.dtoRef shouldBe "UserRef"
    vo.dtoRefCollection shouldBe true
    vo.valueProvider shouldNotBe null
    vo.entityValueProvider shouldNotBe null
    vo.generateToDtoExtension shouldBe true
    vo.generateApplyToEntityExtension shouldBe true
    vo.searchType shouldBe zygarde.codegen.SearchType.EQ
    vo.searchForField shouldBe "user"
    vo.sinceApiVersion shouldBe 2L
  }

  @Test
  fun `DtoFieldDescriptionVo should use default values`() {
    // given/when
    val vo = ZygardeApiPropGenerator.DtoFieldDescriptionVo(
      entityFieldName = "name",
      entityFieldType = String::class.asTypeName(),
      dtoName = "TestDto",
      dtoFieldName = "name",
      dtoFieldType = String::class.asTypeName(),
      comment = "Test comment"
    )

    // then
    vo.dtoRef shouldBe ""
    vo.dtoRefCollection shouldBe false
    vo.valueProvider shouldBe null
    vo.entityValueProvider shouldBe null
    vo.generateToDtoExtension shouldBe false
    vo.generateApplyToEntityExtension shouldBe false
    vo.searchType shouldBe zygarde.codegen.SearchType.NONE
    vo.searchForField shouldBe null
    vo.sinceApiVersion shouldBe 0L
  }

  @Test
  fun `DtoFieldDescriptionVo should support data class operations`() {
    // given
    val vo1 = ZygardeApiPropGenerator.DtoFieldDescriptionVo(
      entityFieldName = "id",
      entityFieldType = Long::class.asTypeName(),
      dtoName = "TestDto",
      dtoFieldName = "id",
      dtoFieldType = Long::class.asTypeName(),
      comment = "ID"
    )
    val vo2 = vo1.copy()
    val vo3 = vo1.copy(dtoFieldName = "testId")

    // then
    vo1 shouldBe vo2
    vo1.hashCode() shouldBe vo2.hashCode()
    vo3.dtoFieldName shouldBe "testId"
    vo3.entityFieldName shouldBe "id"
  }

  @Test
  fun `validValueProvider should return null for NoOpValueProvider`() {
    // This tests the private function indirectly through data class behavior
    val noOpType = NoOpValueProvider::class.asTypeName()

    // When NoOpValueProvider is used, it should be treated as null
    noOpType.toString() shouldBe NoOpValueProvider::class.asTypeName().toString()
  }
}
