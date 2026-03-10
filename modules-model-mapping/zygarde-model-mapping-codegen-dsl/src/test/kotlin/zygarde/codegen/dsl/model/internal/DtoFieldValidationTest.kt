package zygarde.codegen.dsl.model.internal

import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test

class DtoFieldValidationTest {
  @Test
  fun `Regex should generate @field Pattern annotation`() {
    val annotation = DtoFieldValidation.Regex("[a-z]+".toRegex(), "letters only").buildAnnotation()
    val output = annotation.toString()
    output shouldContain "@field:javax.validation.constraints.Pattern"
    output shouldContain "regexp"
    output shouldContain "[a-z]+"
    output shouldContain "letters only"
  }

  @Test
  fun `Email should generate @field Email annotation`() {
    val annotation = DtoFieldValidation.Email("invalid email").buildAnnotation()
    val output = annotation.toString()
    output shouldContain "@field:javax.validation.constraints.Email"
    output shouldContain "invalid email"
  }

  @Test
  fun `Email without message should not include message member`() {
    val annotation = DtoFieldValidation.Email().buildAnnotation()
    val output = annotation.toString()
    output shouldContain "@field:javax.validation.constraints.Email"
    output shouldNotContain "message"
  }

  @Test
  fun `NotNull should generate @field NotNull annotation`() {
    val annotation = DtoFieldValidation.NotNull().buildAnnotation()
    val output = annotation.toString()
    output shouldContain "@field:javax.validation.constraints.NotNull"
  }

  @Test
  fun `NotNull with message should include message`() {
    val annotation = DtoFieldValidation.NotNull("required").buildAnnotation()
    val output = annotation.toString()
    output shouldContain "@field:javax.validation.constraints.NotNull"
    output shouldContain "required"
  }

  @Test
  fun `NotBlank should generate @field NotBlank annotation`() {
    val annotation = DtoFieldValidation.NotBlank().buildAnnotation()
    val output = annotation.toString()
    output shouldContain "@field:javax.validation.constraints.NotBlank"
  }

  @Test
  fun `NotEmpty should generate @field NotEmpty annotation`() {
    val annotation = DtoFieldValidation.NotEmpty().buildAnnotation()
    val output = annotation.toString()
    output shouldContain "@field:javax.validation.constraints.NotEmpty"
  }

  @Test
  fun `Size should generate @field Size annotation with min and max`() {
    val annotation = DtoFieldValidation.Size(min = 1, max = 100, message = "bad size").buildAnnotation()
    val output = annotation.toString()
    output shouldContain "@field:javax.validation.constraints.Size"
    output shouldContain "min"
    output shouldContain "1"
    output shouldContain "max"
    output shouldContain "100"
    output shouldContain "bad size"
  }

  @Test
  fun `Min should generate @field Min annotation`() {
    val annotation = DtoFieldValidation.Min(0).buildAnnotation()
    val output = annotation.toString()
    output shouldContain "@field:javax.validation.constraints.Min"
    output shouldContain "0L"
  }

  @Test
  fun `Max should generate @field Max annotation`() {
    val annotation = DtoFieldValidation.Max(999).buildAnnotation()
    val output = annotation.toString()
    output shouldContain "@field:javax.validation.constraints.Max"
    output shouldContain "999L"
  }

  @Test
  fun `DecimalMin should generate @field DecimalMin annotation`() {
    val annotation = DtoFieldValidation.DecimalMin("0.01", inclusive = false).buildAnnotation()
    val output = annotation.toString()
    output shouldContain "@field:javax.validation.constraints.DecimalMin"
    output shouldContain "0.01"
    output shouldContain "inclusive"
    output shouldContain "false"
  }

  @Test
  fun `DecimalMax should generate @field DecimalMax annotation`() {
    val annotation = DtoFieldValidation.DecimalMax("100.00").buildAnnotation()
    val output = annotation.toString()
    output shouldContain "@field:javax.validation.constraints.DecimalMax"
    output shouldContain "100.00"
  }

  @Test
  fun `Positive should generate @field Positive annotation`() {
    val annotation = DtoFieldValidation.Positive().buildAnnotation()
    val output = annotation.toString()
    output shouldContain "@field:javax.validation.constraints.Positive"
  }

  @Test
  fun `PositiveOrZero should generate @field PositiveOrZero annotation`() {
    val annotation = DtoFieldValidation.PositiveOrZero().buildAnnotation()
    val output = annotation.toString()
    output shouldContain "@field:javax.validation.constraints.PositiveOrZero"
  }

  @Test
  fun `Negative should generate @field Negative annotation`() {
    val annotation = DtoFieldValidation.Negative().buildAnnotation()
    val output = annotation.toString()
    output shouldContain "@field:javax.validation.constraints.Negative"
  }

  @Test
  fun `NegativeOrZero should generate @field NegativeOrZero annotation`() {
    val annotation = DtoFieldValidation.NegativeOrZero().buildAnnotation()
    val output = annotation.toString()
    output shouldContain "@field:javax.validation.constraints.NegativeOrZero"
  }

  @Test
  fun `Past should generate @field Past annotation`() {
    val annotation = DtoFieldValidation.Past().buildAnnotation()
    val output = annotation.toString()
    output shouldContain "@field:javax.validation.constraints.Past"
  }

  @Test
  fun `PastOrPresent should generate @field PastOrPresent annotation`() {
    val annotation = DtoFieldValidation.PastOrPresent().buildAnnotation()
    val output = annotation.toString()
    output shouldContain "@field:javax.validation.constraints.PastOrPresent"
  }

  @Test
  fun `Future should generate @field Future annotation`() {
    val annotation = DtoFieldValidation.Future().buildAnnotation()
    val output = annotation.toString()
    output shouldContain "@field:javax.validation.constraints.Future"
  }

  @Test
  fun `FutureOrPresent should generate @field FutureOrPresent annotation`() {
    val annotation = DtoFieldValidation.FutureOrPresent().buildAnnotation()
    val output = annotation.toString()
    output shouldContain "@field:javax.validation.constraints.FutureOrPresent"
  }
}
