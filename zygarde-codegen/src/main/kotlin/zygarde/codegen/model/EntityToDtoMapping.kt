package zygarde.codegen.model

data class EntityToDtoMapping(
  val entityClass: String,
  val dtoClass: String,
  val fieldMappings: List<EntityFieldToDtoFieldMapping>,
)
