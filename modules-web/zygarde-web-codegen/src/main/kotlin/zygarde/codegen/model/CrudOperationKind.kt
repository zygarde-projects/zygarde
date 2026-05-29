package zygarde.codegen.model

enum class CrudOperationKind {
  LIST,
  GET,
  CREATE,
  UPDATE,
  DELETE,
  MERGE_PATCH,
}
