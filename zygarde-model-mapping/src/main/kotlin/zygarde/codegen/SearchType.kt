package zygarde.codegen

enum class SearchType {
  NONE,
  EQ,
  NOT_EQ,
  LT,
  GT,
  LTE,
  GTE,
  IN_LIST,
  DATE_RANGE,
  DATE_TIME_RANGE,
  KEYWORD,
  STARTS_WITH,
  ENDS_WITH,
  CONTAINS,
  LIST_CONTAINS_ANY,
}
