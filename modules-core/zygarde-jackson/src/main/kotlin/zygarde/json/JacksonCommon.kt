package zygarde.json

import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule

object JacksonCommon {
  private var objectMapper: ObjectMapper = JsonMapper.builder()
    .addModule(kotlinModule())
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
    .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
    .build()

  fun setObjectMapper(mapper: ObjectMapper) {
    objectMapper = if (mapper is JsonMapper) mapper.rebuild().addModule(kotlinModule()).build() else mapper
  }

  fun objectMapper() = objectMapper

  fun <T : Any> withObjectMapper(block: (mapper: ObjectMapper) -> T) = block.invoke(objectMapper())
}
