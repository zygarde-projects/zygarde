package zygarde.codegen.`data`.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import zygarde.`data`.api.OpenApiSortableFields
import zygarde.`data`.api.PagingAndSortingRequest

@Schema
@OpenApiSortableFields(value = ["id", "description"])
public class SearchTodoReq() : PagingAndSortingRequest(), Serializable
