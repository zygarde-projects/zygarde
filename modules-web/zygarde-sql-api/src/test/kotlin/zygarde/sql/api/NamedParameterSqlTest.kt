package zygarde.sql.api

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class NamedParameterSqlTest {
  @Test
  fun `should parse named parameters outside single quoted strings`() {
    val parsed = NamedParameterSql.parse(
      "select ':ignored', ''':still_ignored''' from todo where id = :id and name like :name"
    )

    parsed.sql shouldBe "select ':ignored', ''':still_ignored''' from todo where id = ? and name like ?"
    parsed.parameterNames.shouldContainExactly("id", "name")
  }

  @Test
  fun `should not parse postgres cast syntax as named parameter`() {
    val parsed = NamedParameterSql.parse(
      "select created_at::date as created_date from todo where owner_id = :ownerId"
    )

    parsed.sql shouldBe "select created_at::date as created_date from todo where owner_id = ?"
    parsed.parameterNames.shouldContainExactly("ownerId")
  }

  @Test
  fun `should not parse parameters in SQL comments or quoted identifiers`() {
    val parsed = NamedParameterSql.parse(
      """
      select ":ignored_column", `:ignored_backtick`, [:ignored_bracket]
      from todo
      -- :ignored_line
      where id = :id
        /* :ignored_block */
        and name = :name
      """.trimIndent()
    )

    parsed.parameterNames.shouldContainExactly("id", "name")
  }
}
