package zygarde.test

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow
import org.junit.jupiter.api.Test
import zygarde.json.patch.MergePatchField

class MergePatchFieldTest {
  data class PatchReq(
    val name: MergePatchField<String> = MergePatchField.Absent,
    val price: MergePatchField<Int> = MergePatchField.Absent,
  )

  data class StrictPatchReq(
    val name: MergePatchField<String> = MergePatchField.Absent,
  ) {
    @JsonAnySetter
    fun rejectUnknownPatchField(
      fieldName: String,
      @Suppress("UNUSED_PARAMETER") value: Any?,
    ) {
      throw IllegalArgumentException("Unknown JSON merge patch field '$fieldName'")
    }
  }

  private val objectMapper = jacksonObjectMapper()

  @Test
  fun `should keep field absent when JSON field is missing`() {
    val req = objectMapper.readValue<PatchReq>("{}")

    req.name shouldBe MergePatchField.Absent
    req.price shouldBe MergePatchField.Absent
  }

  @Test
  fun `should keep explicit null as NullValue`() {
    val req = objectMapper.readValue<PatchReq>("""{"name":null}""")

    req.name shouldBe MergePatchField.NullValue
    req.price shouldBe MergePatchField.Absent
  }

  @Test
  fun `should deserialize typed value`() {
    val req = objectMapper.readValue<PatchReq>("""{"name":"book","price":100}""")

    req.name shouldBe MergePatchField.Value("book")
    req.price shouldBe MergePatchField.Value(100)
  }

  @Test
  fun `should reject unknown field with JsonAnySetter even when object mapper allows unknown properties`() {
    val tolerantObjectMapper = jacksonObjectMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    shouldThrow<JsonMappingException> {
      tolerantObjectMapper.readValue<StrictPatchReq>("""{"unknown":100}""")
    }
  }
}
