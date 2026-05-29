package zygarde.codegen.dsl.sqlapi

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName

class SqlApiDataProviderFieldDsl(
  private val name: String,
  private val providerType: ClassName,
  private val keyType: TypeName,
  private val valueType: TypeName,
) {
  private var keyColumnName: String? = null
  private var nullable: Boolean = false
  private var description: String = ""

  fun keyColumn(name: String) {
    keyColumnName = name
  }

  fun nullable() {
    nullable = true
  }

  fun description(value: String) {
    description = value
  }

  fun toSqlApiDataProviderField(): SqlApiDataProviderField {
    return SqlApiDataProviderField(
      name = name,
      providerType = providerType,
      keyType = keyType,
      valueType = valueType.copy(nullable = nullable),
      keyColumnName = requireNotNull(keyColumnName) { "SQL API provider field '$name' requires keyColumn(name)." },
      nullable = nullable,
      description = description,
    )
  }
}
