package zygarde.codegen.generator.shared

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asTypeName
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import zygarde.codegen.SearchType

class ApiPropSharedGeneratorTest {
  private val entityTypeName = ClassName("com.example", "MyEntity")
  private val dtoPackageName = "com.example.dto"
  private val searchPackageName = "com.example.entity.search"

  @Test
  fun `validValueProvider should return null for NoOpValueProvider type`() {
    // given
    val noOpType = ClassName("zygarde.codegen.value", "NoOpValueProvider")

    // when
    val result = noOpType.validValueProvider()

    // then
    result shouldBe null
  }

  @Test
  fun `validValueProvider should return the type when not NoOpValueProvider`() {
    // given
    val customProvider = ClassName("com.example", "MyValueProvider")

    // when
    val result = customProvider.validValueProvider()

    // then
    result shouldBe customProvider
  }

  @Test
  fun `validValueProvider should return null for null input`() {
    // given
    val nullType: com.squareup.kotlinpoet.TypeName? = null

    // when
    val result = nullType.validValueProvider()

    // then
    result shouldBe null
  }

  @Test
  fun `generateToDtoExtensionFunction should create function with correct name and receiver`() {
    // given
    val descriptions = listOf(
      DtoFieldDescriptionVo(
        entityFieldName = "name",
        entityFieldType = String::class.asTypeName(),
        dtoName = "MyDto",
        dtoFieldName = "name",
        dtoFieldType = String::class.asTypeName(),
        comment = "Name",
        generateToDtoExtension = true
      )
    )

    // when
    val funSpec = ApiPropSharedGenerator.generateToDtoExtensionFunction(
      entityTypeName,
      dtoPackageName,
      "MyDto",
      descriptions
    )

    // then
    funSpec.name shouldBe "toMyDto"
    funSpec.receiverType shouldBe entityTypeName
    funSpec.returnType shouldBe ClassName(dtoPackageName, "MyDto")
  }

  @Test
  fun `generateApplyToEntityExtensionFunction should create function with correct name`() {
    // given
    val descriptions = listOf(
      DtoFieldDescriptionVo(
        entityFieldName = "name",
        entityFieldType = String::class.asTypeName(),
        dtoName = "CreateReq",
        dtoFieldName = "name",
        dtoFieldType = String::class.asTypeName(),
        comment = "Name",
        generateApplyToEntityExtension = true
      )
    )

    // when
    val funSpec = ApiPropSharedGenerator.generateApplyToEntityExtensionFunction(
      entityTypeName,
      dtoPackageName,
      "CreateReq",
      descriptions
    )

    // then
    funSpec.name shouldBe "applyFromCreateReq"
    funSpec.receiverType shouldBe entityTypeName
    funSpec.returnType shouldBe entityTypeName
  }

  @Test
  fun `generateApplyToEntityExtensionFunction should handle sinceApiVersion`() {
    // given
    val descriptions = listOf(
      DtoFieldDescriptionVo(
        entityFieldName = "name",
        entityFieldType = String::class.asTypeName(),
        dtoName = "UpdateReq",
        dtoFieldName = "name",
        dtoFieldType = String::class.asTypeName(),
        comment = "Name",
        generateApplyToEntityExtension = true,
        sinceApiVersion = 2
      )
    )

    // when
    val funSpec = ApiPropSharedGenerator.generateApplyToEntityExtensionFunction(
      entityTypeName,
      dtoPackageName,
      "UpdateReq",
      descriptions
    )

    // then
    funSpec.toString() shouldContain "apiVersion"
    funSpec.toString() shouldContain "ApiVersionContext"
  }

  @Test
  fun `generateSearchExtensionFunction should create function for EQ search`() {
    // given
    val descriptions = listOf(
      DtoFieldDescriptionVo(
        entityFieldName = "status",
        entityFieldType = String::class.asTypeName(),
        dtoName = "SearchReq",
        dtoFieldName = "status",
        dtoFieldType = String::class.asTypeName().copy(nullable = true),
        comment = "Status",
        searchType = SearchType.EQ
      )
    )

    // when
    val funSpec = ApiPropSharedGenerator.generateSearchExtensionFunction(
      entityTypeName,
      dtoPackageName,
      searchPackageName,
      "SearchReq",
      descriptions
    )

    // then
    funSpec.name shouldBe "applyFromSearchReq"
    funSpec.toString() shouldContain "eq"
  }

  @Test
  fun `buildDtoClassesAndExtensions should produce dto specs and extension builder`() {
    // given
    val descriptions = listOf(
      DtoFieldDescriptionVo(
        entityFieldName = "name",
        entityFieldType = String::class.asTypeName(),
        dtoName = "TestDto",
        dtoFieldName = "name",
        dtoFieldType = String::class.asTypeName(),
        comment = "Name",
        generateToDtoExtension = true
      ),
      DtoFieldDescriptionVo(
        entityFieldName = "age",
        entityFieldType = Int::class.asTypeName(),
        dtoName = "TestDto",
        dtoFieldName = "age",
        dtoFieldType = Int::class.asTypeName(),
        comment = "Age",
        generateToDtoExtension = true
      )
    )

    // when
    val (dtoSpecs, extensionBuilder) = ApiPropSharedGenerator.buildDtoClassesAndExtensions(
      entityTypeName = entityTypeName,
      dtoPackageName = dtoPackageName,
      searchPackageName = searchPackageName,
      dtoExtensionName = "MyEntityDtoExtensions",
      allDescriptions = descriptions,
      dtoInheritMap = emptyMap()
    )

    // then
    dtoSpecs.size shouldBe 1
    dtoSpecs["TestDto"] shouldNotBe null
    val dtoSpec = dtoSpecs["TestDto"]!!
    dtoSpec.name shouldBe "TestDto"
    dtoSpec.propertySpecs.size shouldBe 2
    dtoSpec.propertySpecs.map { it.name } shouldBe listOf("name", "age")

    val extensionFile = extensionBuilder.build()
    extensionFile.name shouldBe "MyEntityDtoExtensions"
    extensionFile.members.size shouldBe 1 // toTestDto function
  }

  @Test
  fun `buildDtoClassesAndExtensions should handle multiple DTOs`() {
    // given
    val descriptions = listOf(
      DtoFieldDescriptionVo(
        entityFieldName = "name",
        entityFieldType = String::class.asTypeName(),
        dtoName = "DtoA",
        dtoFieldName = "name",
        dtoFieldType = String::class.asTypeName(),
        comment = "Name",
        generateToDtoExtension = true
      ),
      DtoFieldDescriptionVo(
        entityFieldName = "name",
        entityFieldType = String::class.asTypeName(),
        dtoName = "DtoB",
        dtoFieldName = "name",
        dtoFieldType = String::class.asTypeName(),
        comment = "Name",
        generateApplyToEntityExtension = true
      )
    )

    // when
    val (dtoSpecs, _) = ApiPropSharedGenerator.buildDtoClassesAndExtensions(
      entityTypeName = entityTypeName,
      dtoPackageName = dtoPackageName,
      searchPackageName = searchPackageName,
      dtoExtensionName = "MyEntityDtoExtensions",
      allDescriptions = descriptions,
      dtoInheritMap = emptyMap()
    )

    // then
    dtoSpecs.size shouldBe 2
    dtoSpecs.keys shouldBe setOf("DtoA", "DtoB")
  }

  @Test
  fun `buildDtoClassesAndExtensions should apply superclass from inherit map`() {
    // given
    val superClass = ClassName("com.example", "BaseDto")
    val descriptions = listOf(
      DtoFieldDescriptionVo(
        entityFieldName = "id",
        entityFieldType = Long::class.asTypeName(),
        dtoName = "MyDto",
        dtoFieldName = "id",
        dtoFieldType = Long::class.asTypeName(),
        comment = "ID"
      )
    )

    // when
    val (dtoSpecs, _) = ApiPropSharedGenerator.buildDtoClassesAndExtensions(
      entityTypeName = entityTypeName,
      dtoPackageName = dtoPackageName,
      searchPackageName = searchPackageName,
      dtoExtensionName = "MyEntityDtoExtensions",
      allDescriptions = descriptions,
      dtoInheritMap = mapOf("MyDto" to superClass)
    )

    // then
    val dtoSpec = dtoSpecs["MyDto"]!!
    dtoSpec.superclass shouldBe superClass
  }
}
