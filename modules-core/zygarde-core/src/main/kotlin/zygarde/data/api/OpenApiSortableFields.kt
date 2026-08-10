package zygarde.data.api

/**
 * Documents the values supported by [SortField.field] for this request type.
 * This annotation does not enforce the values during request deserialization.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class OpenApiSortableFields(
  vararg val value: String,
)
