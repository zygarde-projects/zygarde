package zygarde.codegen.dsl.model.internal

import com.squareup.kotlinpoet.asTypeName
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import zygarde.codegen.meta.CodegenDtoSimple
import zygarde.codegen.meta.ModelMetaField

class DtoFieldMappingValidationDslTest {
  private val testDto = object : CodegenDtoSimple {
    override val name: String = "TestDto"
  }

  private fun createMapping(): DtoFieldMapping.DtoFieldNoMapping {
    return DtoFieldMapping.DtoFieldNoMapping(
      modelField = ModelMetaField(
        modelClass = String::class.asTypeName(),
        fieldName = "test",
        fieldClass = String::class.asTypeName(),
        fieldNullable = false
      ),
      dto = testDto
    )
  }

  @Test
  fun `notNull should add NotNull validation`() {
    val mapping = createMapping()
    mapping.notNull("required")
    mapping.validations.size shouldBe 1
    mapping.validations[0].shouldBeInstanceOf<DtoFieldValidation.NotNull>()
    mapping.validations[0].message shouldBe "required"
  }

  @Test
  fun `notBlank should add NotBlank validation`() {
    val mapping = createMapping()
    mapping.notBlank()
    mapping.validations.size shouldBe 1
    mapping.validations[0].shouldBeInstanceOf<DtoFieldValidation.NotBlank>()
  }

  @Test
  fun `notEmpty should add NotEmpty validation`() {
    val mapping = createMapping()
    mapping.notEmpty()
    mapping.validations.size shouldBe 1
    mapping.validations[0].shouldBeInstanceOf<DtoFieldValidation.NotEmpty>()
  }

  @Test
  fun `email should add Email validation`() {
    val mapping = createMapping()
    mapping.email("invalid")
    mapping.validations.size shouldBe 1
    mapping.validations[0].shouldBeInstanceOf<DtoFieldValidation.Email>()
    mapping.validations[0].message shouldBe "invalid"
  }

  @Test
  fun `regex should add Regex validation`() {
    val mapping = createMapping()
    mapping.regex("[a-z]+".toRegex(), "letters only")
    mapping.validations.size shouldBe 1
    val v = mapping.validations[0]
    v.shouldBeInstanceOf<DtoFieldValidation.Regex>()
    v.regexp.pattern shouldBe "[a-z]+"
  }

  @Test
  fun `size should add Size validation`() {
    val mapping = createMapping()
    mapping.size(min = 1, max = 50)
    mapping.validations.size shouldBe 1
    val v = mapping.validations[0]
    v.shouldBeInstanceOf<DtoFieldValidation.Size>()
    v.min shouldBe 1
    v.max shouldBe 50
  }

  @Test
  fun `min and max should add respective validations`() {
    val mapping = createMapping()
    mapping.min(0)
    mapping.max(100)
    mapping.validations.size shouldBe 2
    mapping.validations[0].shouldBeInstanceOf<DtoFieldValidation.Min>()
    mapping.validations[1].shouldBeInstanceOf<DtoFieldValidation.Max>()
  }

  @Test
  fun `decimalMin and decimalMax should add respective validations`() {
    val mapping = createMapping()
    mapping.decimalMin("0.01", inclusive = false)
    mapping.decimalMax("99.99")
    mapping.validations.size shouldBe 2
    val min = mapping.validations[0]
    min.shouldBeInstanceOf<DtoFieldValidation.DecimalMin>()
    min.value shouldBe "0.01"
    min.inclusive shouldBe false
  }

  @Test
  fun `positive and positiveOrZero should add respective validations`() {
    val mapping = createMapping()
    mapping.positive()
    mapping.positiveOrZero()
    mapping.validations.size shouldBe 2
    mapping.validations[0].shouldBeInstanceOf<DtoFieldValidation.Positive>()
    mapping.validations[1].shouldBeInstanceOf<DtoFieldValidation.PositiveOrZero>()
  }

  @Test
  fun `negative and negativeOrZero should add respective validations`() {
    val mapping = createMapping()
    mapping.negative()
    mapping.negativeOrZero()
    mapping.validations.size shouldBe 2
    mapping.validations[0].shouldBeInstanceOf<DtoFieldValidation.Negative>()
    mapping.validations[1].shouldBeInstanceOf<DtoFieldValidation.NegativeOrZero>()
  }

  @Test
  fun `temporal validations should add respective validations`() {
    val mapping = createMapping()
    mapping.past()
    mapping.pastOrPresent()
    mapping.future()
    mapping.futureOrPresent()
    mapping.validations.size shouldBe 4
    mapping.validations[0].shouldBeInstanceOf<DtoFieldValidation.Past>()
    mapping.validations[1].shouldBeInstanceOf<DtoFieldValidation.PastOrPresent>()
    mapping.validations[2].shouldBeInstanceOf<DtoFieldValidation.Future>()
    mapping.validations[3].shouldBeInstanceOf<DtoFieldValidation.FutureOrPresent>()
  }

  @Test
  fun `multiple validations can be chained`() {
    val mapping = createMapping()
    mapping.notBlank("must not be blank")
    mapping.size(min = 1, max = 255)
    mapping.regex("[a-zA-Z ]+".toRegex(), "letters only")
    mapping.validations.size shouldBe 3
  }
}
