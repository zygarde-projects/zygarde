package zygarde.codegen

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ZygardeModelMappingKaptOptionsTest {

  @Test
  fun `should have correct MODEL_MAPPING_DTO_WRITE_TO value`() {
    ZygardeModelMappingKaptOptions.MODEL_MAPPING_DTO_WRITE_TO shouldBe "zygarde.model_mapping.dto.write_to"
  }

  @Test
  fun `should have correct MODEL_MAPPING_EXTENSION_WRITE_TO value`() {
    ZygardeModelMappingKaptOptions.MODEL_MAPPING_EXTENSION_WRITE_TO shouldBe "zygarde.model_mapping.extension.write_to"
  }

  @Test
  fun `should have correct ENTITY_PACKAGE_SEARCH value`() {
    ZygardeModelMappingKaptOptions.ENTITY_PACKAGE_SEARCH shouldBe "zygarde.codegen.entity.search"
  }

  @Test
  fun `should have all expected options`() {
    // Verify the object has exactly 3 properties
    val fields = ZygardeModelMappingKaptOptions::class.java.declaredFields
    val constants = fields.filter { field ->
      java.lang.reflect.Modifier.isStatic(field.modifiers) &&
        java.lang.reflect.Modifier.isFinal(field.modifiers) &&
        field.type == String::class.java
    }
    constants.size shouldBe 3
  }
}
