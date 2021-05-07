package zygarde.codegen.model

data class EntityToDtoMapping(
  val entityClassFullName: String,
  val dtoClassName: String,
  val fieldMappings: List<EntityFieldToDtoFieldMapping>,
)
