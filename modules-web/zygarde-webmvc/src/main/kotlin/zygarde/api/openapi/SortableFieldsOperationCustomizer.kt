package zygarde.api.openapi

import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import org.springdoc.core.customizers.OperationCustomizer
import org.springframework.web.method.HandlerMethod
import zygarde.data.api.OpenApiSortableFields
import zygarde.data.api.SortDirection

class SortableFieldsOperationCustomizer : OperationCustomizer {
  override fun customize(operation: Operation, handlerMethod: HandlerMethod): Operation {
    val sortableFields = handlerMethod.methodParameters
      .asSequence()
      .mapNotNull { it.parameterType.getAnnotation(OpenApiSortableFields::class.java) }
      .flatMap { it.value.asSequence() }
      .distinct()
      .toList()
      .takeIf { it.isNotEmpty() }
      ?: return operation

    operation.requestBody
      ?.content
      ?.values
      ?.forEach { mediaType ->
        mediaType.schema = mediaType.schema?.withSortableFields(sortableFields)
      }

    operation.parameters
      .orEmpty()
      .filter { it.name == "sorts.field" || it.name == "sorts[].field" }
      .forEach { parameter -> parameter.schema?.setEnum(sortableFields) }

    operation.parameters
      .orEmpty()
      .filter { it.name == "sorts" && it.schema is ArraySchema }
      .forEach { parameter -> (parameter.schema as ArraySchema).items = sortableFieldSchema(sortableFields) }

    return operation
  }

  private fun Schema<*>.withSortableFields(sortableFields: List<String>): Schema<*> {
    if (containsSortableFields(sortableFields)) {
      return this
    }
    val overlay = ObjectSchema().apply {
      addProperty(
        "sorts",
        ArraySchema().apply {
          items = sortableFieldSchema(sortableFields)
        }
      )
    }
    return ComposedSchema().apply {
      addAllOfItem(this@withSortableFields)
      addAllOfItem(overlay)
    }
  }

  private fun Schema<*>.containsSortableFields(sortableFields: List<String>): Boolean {
    val sorts = properties?.get("sorts") as? ArraySchema
    val field = sorts?.items?.properties?.get("field")
    if (field?.enum == sortableFields) {
      return true
    }
    return allOf.orEmpty().any { it.containsSortableFields(sortableFields) }
  }

  private fun sortableFieldSchema(sortableFields: List<String>): ObjectSchema {
    return ObjectSchema().apply {
      addProperty(
        "sort",
        StringSchema().apply {
          description = "排序方式"
          setEnum(SortDirection.entries.map { it.name })
        }
      )
      addProperty(
        "field",
        StringSchema().apply {
          description = "排序欄位"
          setEnum(sortableFields)
        }
      )
    }
  }
}
